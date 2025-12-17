package com.viecinema.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentResponse {
    private String code;           // 00 = success
    private String message;
    private String paymentUrl;     // URL redirect đến VNPay
}