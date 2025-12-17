package com.viecinema.payment.service;

import com.viecinema.common.enums.PaymentStatus;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.payment.dto.response.PaymentInfoResponse;
import com.viecinema.payment.entity.Payment;
import com.viecinema.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VnpayService vnpayService;

    public PaymentService(PaymentRepository paymentRepository, @Lazy VnpayService vnpayService) {
        this.paymentRepository = paymentRepository;
        this.vnpayService = vnpayService;
    }

    /**
     * Lấy thông tin thanh toán từ booking ID
     *
     * @param bookingId ID của booking
     * @param httpRequest HttpServletRequest để tạo payment URL nếu cần
     * @return PaymentInfoResponse thông tin thanh toán
     */
    @Transactional(readOnly = true)
    public PaymentInfoResponse getPaymentByBookingId(Integer bookingId, HttpServletRequest httpRequest) {
        log.info("Getting payment info for booking ID: {}", bookingId);

        Payment payment = paymentRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking ID: " + bookingId));

        String paymentUrl = null;

        // Nếu payment đang ở trạng thái PENDING và method là vnpay, tạo lại payment URL từ payment hiện tại
        if (PaymentStatus.PENDING.equals(payment.getStatus()) && "vnpay".equalsIgnoreCase(payment.getMethod())) {
            try {
                // Sử dụng phương thức không transactional để build URL từ payment đã tồn tại
                paymentUrl = vnpayService.buildPaymentUrlForExistingPayment(payment, httpRequest);
            } catch (Exception e) {
                log.error("Error creating payment URL for booking {}: {}", bookingId, e.getMessage());
                // Không throw exception, chỉ log lỗi và trả về null cho paymentUrl
            }
        }

        return PaymentInfoResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking().getId())
                .bookingCode(payment.getBooking().getBookingCode())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentUrl(paymentUrl)
                .transactionTime(payment.getTransactionTime())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
