package com.viecinema.payment.service;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.common.enums.PaymentStatus;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.common.util.VnpayUtil;
import com.viecinema.config.VnpayConfig;
import com.viecinema.payment.dto.request.VnpayPaymentRequest;
import com.viecinema.payment.dto.response.VnpayCallbackResponse;
import com.viecinema.payment.dto.response.VnpayPaymentResponse;
import com.viecinema.payment.entity.Payment;
import com.viecinema.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayService {

    private final VnpayConfig vnpayConfig;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public VnpayPaymentResponse createPayment(
            Integer bookingId,
            VnpayPaymentRequest request,
            HttpServletRequest httpRequest) {

        log.info("Creating VNPay payment for booking {}", bookingId);

        // 1. Validate booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!BookingStatus.PENDING.equals(booking.getStatus())) {
            throw new SpecificBusinessException("Booking đã được thanh toán hoặc đã hủy");
        }

        // 1.Check if the booking has expired (10 minutes)
        if (booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new SpecificBusinessException("Booking đã hết hạn thanh toán");
        }

        // 2. Create payment record with PENDING status
        String txnRef = VnpayUtil.generateTxnRef();

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getFinalAmount())
                .method("vnpay")
                .status(PaymentStatus.PENDING)
                .transactionId(txnRef)
                .build();

        paymentRepository.save(payment);

        // 3. Build VNPay parameters
        Map<String, String> vnpParams = new HashMap<>();

        vnpParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpParams.put("vnp_Command", vnpayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());

        // Amount in VND, multiplied by 100 (e.g. 100.50 VND -> 10050)
        long amount = booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));

        vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrencyCode());

        // Bank code (optional)
        if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
            vnpParams.put("vnp_BankCode", request.getBankCode());
        }

        vnpParams.put("vnp_TxnRef", txnRef);

        // Order info
        String orderInfo = request.getOrderInfo() != null
                ? request.getOrderInfo()
                : "Thanh toan ve xem phim - " + booking.getBookingCode();
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "billpayment");

        // Locale
        String locale = request.getLocale() != null ? request.getLocale() : vnpayConfig.getLocale();
        vnpParams.put("vnp_Locale", locale);

        // Return URL
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());

        // IP Address
        String ipAddress = VnpayUtil.getIpAddress(httpRequest);
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Create date & Expire date
        Date now = new Date();
        vnpParams.put("vnp_CreateDate", VnpayUtil.formatDateTime(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, vnpayConfig.getTimeout());
        vnpParams.put("vnp_ExpireDate", VnpayUtil.formatDateTime(calendar.getTime()));

        // 4. Calculate secure hash
        String hashData = VnpayUtil.hashAllFields(vnpParams);
        String secureHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", secureHash);

        // 5. Build payment URL
        String queryString = VnpayUtil.buildQueryString(vnpParams);
        String paymentUrl = vnpayConfig.getApiUrl() + "?" + queryString;

        log.info("VNPay payment URL created for booking {}: {}", bookingId, paymentUrl);

        return VnpayPaymentResponse.builder()
                .code("00")
                .message("Success")
                .paymentUrl(paymentUrl)
                .build();
    }

    public String buildPaymentUrlForExistingPayment(com.viecinema.payment.entity.Payment payment, jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Building VNPay payment URL for existing payment: {}", payment.getPaymentId());

        java.util.Map<String, String> vnpParams = new java.util.HashMap<>();

        vnpParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpParams.put("vnp_Command", vnpayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());

        long amount = payment.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));

        vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrencyCode());

        // Use existing txn ref
        vnpParams.put("vnp_TxnRef", payment.getTransactionId());

        // Order info
        String orderInfo = "Thanh toan ve xem phim - " + payment.getBooking().getBookingCode();
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "billpayment");

        // Locale
        vnpParams.put("vnp_Locale", vnpayConfig.getLocale());

        // Return URL
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());

        // IP Address
        String ipAddress = com.viecinema.common.util.VnpayUtil.getIpAddress(httpRequest);
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Create date & Expire date
        java.util.Date now = new java.util.Date();
        vnpParams.put("vnp_CreateDate", com.viecinema.common.util.VnpayUtil.formatDateTime(now));

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(java.util.Calendar.MINUTE, vnpayConfig.getTimeout());
        vnpParams.put("vnp_ExpireDate", com.viecinema.common.util.VnpayUtil.formatDateTime(calendar.getTime()));

        // Secure hash
        String hashData = com.viecinema.common.util.VnpayUtil.hashAllFields(vnpParams);
        String secureHash = com.viecinema.common.util.VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", secureHash);

        String queryString2 = com.viecinema.common.util.VnpayUtil.buildQueryString(vnpParams);
        return vnpayConfig.getApiUrl() + "?" + queryString2;
    }

    @Transactional
    public VnpayCallbackResponse handleCallback(Map<String, String> params) {
        log.info("Received VNPay callback: {}", params);

        // 1. Validate secure hash
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        String hashData = VnpayUtil.hashAllFields(params);
        String calculatedHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

        if (!calculatedHash.equals(vnpSecureHash)) {
            log.error("Invalid secure hash.  Expected: {}, Got: {}", calculatedHash, vnpSecureHash);
            return VnpayCallbackResponse.builder()
                    .code("97")
                    .message("Invalid signature")
                    .build();
        }

        // 2. Extract parameters
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String bankTranNo = params.get("vnp_BankTranNo");
        String cardType = params.get("vnp_CardType");
        String payDate = params.get("vnp_PayDate");

        // 3. Find payment by transaction ID
        Payment payment = paymentRepository.findByTransactionId(txnRef);

        // Check if payment exists
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            log.warn("Payment {} already processed with status: {}", txnRef, payment.getStatus());
            return VnpayCallbackResponse.builder()
                    .code("02")
                    .message("Order already confirmed")
                    .bookingCode(payment.getBooking().getBookingCode())
                    .build();
        }

        // 4. Handle payment result
        Booking booking = payment.getBooking();

        if ("00".equals(responseCode)) {
            // Payment successful
            payment.setStatus(PaymentStatus.SUCCESS);
            // include details in gateway response
            payment.setGatewayResponse(String.format("response=%s, bankCode=%s, bankTranNo=%s, cardType=%s, payDate=%s",
                    params, bankCode, bankTranNo, cardType, payDate));

            booking.setStatus(BookingStatus.PAID);

            // Generate QR code data for the booking
            String qrData = generateQRCodeData(booking);
            booking.setQrCodeData(qrData);

            bookingRepository.save(booking);
            paymentRepository.save(payment);

            log.info("Payment success for booking {}", booking.getBookingCode());

            return VnpayCallbackResponse.builder()
                    .code("00")
                    .message("Payment successful")
                    .bookingCode(booking.getBookingCode())
                    .transactionNo(transactionNo)
                    .build();

        } else {
            // Payment failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(String.format("response=%s, bankCode=%s, bankTranNo=%s, cardType=%s, payDate=%s",
                    params, bankCode, bankTranNo, cardType, payDate));

            booking.setStatus(BookingStatus.CANCELLED);

            bookingRepository.save(booking);
            paymentRepository.save(payment);

            log.warn("Payment failed for booking {}. Response code: {}", booking.getBookingCode(), responseCode);

            return VnpayCallbackResponse.builder()
                    .code(responseCode)
                    .message(getResponseMessage(responseCode))
                    .bookingCode(booking.getBookingCode())
                    .build();
        }
    }

    @Transactional
    public Map<String, String> handleIPN(Map<String, String> params) {
        log.info("Received VNPay IPN: {}", params);

        Map<String, String> response = new HashMap<>();

        try {
            // Validate secure hash
            String vnpSecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            String hashData = VnpayUtil.hashAllFields(params);
            String calculatedHash = VnpayUtil.hmacSHA512(vnpayConfig.getHashSecret(), hashData);

            if (!calculatedHash.equals(vnpSecureHash)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
                return response;
            }

            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");

            Payment payment = paymentRepository.findByTransactionId(txnRef);

            // Check if the order has been confirmed.
            if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // Validate amount
            long vnpAmount = Long.parseLong(params.get("vnp_Amount"));
            long orderAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            if (vnpAmount != orderAmount) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return response;
            }

            // Update payment and booking status based on response code
            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.getBooking().setStatus(BookingStatus.PAID);

                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.getBooking().setStatus(BookingStatus.CANCELLED);

                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
            }

            paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Error processing IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }

        return response;
    }

    // ========== HELPER METHODS ==========

    private String generateQRCodeData(Booking booking) {
        // Format:  BOOKING_CODE|SHOWTIME_ID|USER_ID|TIMESTAMP
        return String.format("%s|%d|%d|%d",
                booking.getBookingCode(),
                booking.getShowtime().getId(),
                booking.getUser().getId(),
                System.currentTimeMillis()
        );
    }

    private String getResponseMessage(String responseCode) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Payment successful");
        messages.put("07", "Deduction successful. Transaction is suspicious (related to fraud, unusual transaction)");
        messages.put("09", "Payment unsuccessful due to: The customer's card/account has not registered for Internet Banking services at the bank.");
        messages.put("10", "Payment unsuccessful due to: Customers who provide incorrect card/account information verification more than 3 times.");
        messages.put("11", "Payment unsuccessful due to: The payment deadline has expired.");
        messages.put("12", "Payment unsuccessful due to: The customer's card/account has been locked.");
        messages.put("13", "Payment unsuccessful due to: You have entered the wrong transaction authentication password (OTP).");
        messages.put("24", "Payment unsuccessful due to: Customer cancels transaction.");
        messages.put("51", "Payment unsuccessful due to: The customer's card/account has insufficient funds.");
        messages.put("65", "Payment unsuccessful due to: The customer's card/account has reached the limit of allowed transactions.");
        messages.put("75", "The clearing bank is undergoing maintenance.");
        messages.put("79", "Payment unsuccessful due to: Customer enters incorrect payment password too many times.");
        messages.put("99", "Payment unsuccessful due to: Other errors.");

        return messages.getOrDefault(responseCode, "Unknown response code");
    }
}
