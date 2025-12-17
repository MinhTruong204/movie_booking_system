package com.viecinema.payment.service;

import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.payment.dto.response.PaymentInfoResponse;
import com.viecinema.payment.entity.Payment;
import com.viecinema.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Lấy thông tin thanh toán từ booking ID
     *
     * @param bookingId ID của booking
     * @return PaymentInfoResponse thông tin thanh toán
     */
    @Transactional(readOnly = true)
    public PaymentInfoResponse getPaymentByBookingId(Integer bookingId) {
        log.info("Getting payment info for booking ID: {}", bookingId);

        Payment payment = paymentRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking ID: " + bookingId));

        return PaymentInfoResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking().getId())
                .bookingCode(payment.getBooking().getBookingCode())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .transactionTime(payment.getTransactionTime())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

