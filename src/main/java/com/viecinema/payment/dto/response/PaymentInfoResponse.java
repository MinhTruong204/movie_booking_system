package com.viecinema.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoResponse {
    private Integer paymentId;
    private Integer bookingId;
    private String bookingCode;
    private BigDecimal amount;
    private String method; // vnpay, momo, zalopay
    private PaymentStatus status;
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

