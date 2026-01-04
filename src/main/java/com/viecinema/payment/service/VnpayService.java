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

    /**
     * Tạo URL thanh toán VNPay
     */
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

        // Kiểm tra booking có hết hạn chưa (10 phút)
        if (booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new SpecificBusinessException("Booking đã hết hạn thanh toán");
        }

        // 2. Tạo payment record
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

        // Amount phải nhân 100 (VNPay yêu cầu đơn vị là xu, không có dấu phẩy)
        long amount = booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));

        vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrencyCode());

        // Bank code (nếu có)
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

        // 4. Tạo secure hash
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

    /**
     * Tạo payment URL từ một Payment đã tồn tại (không lưu mới)
     * Không đánh dấu transactional để tránh ghi vào DB khi đang trong transaction read-only
     */
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

    /**
     * Xử lý callback từ VNPay (Return URL)
     */
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

        // 2. Lấy thông tin từ params
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String bankTranNo = params.get("vnp_BankTranNo");
        String cardType = params.get("vnp_CardType");
        String payDate = params.get("vnp_PayDate");

        // 3. Tìm payment
        Payment payment = paymentRepository.findByTransactionId(txnRef);

        // Kiểm tra xem đã xử lý chưa (tránh duplicate)
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            log.warn("Payment {} already processed with status: {}", txnRef, payment.getStatus());
            return VnpayCallbackResponse.builder()
                    .code("02")
                    .message("Order already confirmed")
                    .bookingCode(payment.getBooking().getBookingCode())
                    .build();
        }

        // 4. Xử lý kết quả
        Booking booking = payment.getBooking();

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            payment.setStatus(PaymentStatus.SUCCESS);
            // include details in gateway response
            payment.setGatewayResponse(String.format("response=%s, bankCode=%s, bankTranNo=%s, cardType=%s, payDate=%s",
                    params, bankCode, bankTranNo, cardType, payDate));

            booking.setStatus(BookingStatus.PAID);

            // Tạo QR code cho vé
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
            // Thanh toán thất bại
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

            // Kiểm tra order đã được confirm chưa
            if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // Kiểm tra số tiền
            long vnpAmount = Long.parseLong(params.get("vnp_Amount"));
            long orderAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            if (vnpAmount != orderAmount) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return response;
            }

            // Xử lý kết quả
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

    /**
     * Query transaction status từ VNPay
     */
    public Map<String, String> queryTransaction(String txnRef, String transactionDate) {
        // Log parameters to avoid unused-parameter warnings and help debugging
        log.info("queryTransaction called with txnRef={} transactionDate={}", txnRef, transactionDate);

        // TODO: Implement query API của VNPay nếu cần
        // Cần call API:  https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
        throw new UnsupportedOperationException("Not implemented yet");
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
        messages.put("00", "Giao dịch thành công");
        messages.put("07", "Trừ tiền thành công.  Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)");
        messages.put("09", "Giao dịch không thành công do:  Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng");
        messages.put("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        messages.put("11", "Giao dịch không thành công do:  Đã hết hạn chờ thanh toán");
        messages.put("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa");
        messages.put("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)");
        messages.put("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch");
        messages.put("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch");
        messages.put("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày");
        messages.put("75", "Ngân hàng thanh toán đang bảo trì");
        messages.put("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định");
        messages.put("99", "Các lỗi khác");

        return messages.getOrDefault(responseCode, "Lỗi không xác định");
    }
}
