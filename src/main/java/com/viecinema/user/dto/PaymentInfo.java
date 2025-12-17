package com.viecinema.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private Integer paymentId;
    private String method; // vnpay, momo, zalopay
    private String status; // pending, success, failed
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm: ss")
    private LocalDateTime transactionTime;
}
