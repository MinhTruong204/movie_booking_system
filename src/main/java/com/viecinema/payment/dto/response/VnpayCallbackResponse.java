package com.viecinema.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCallbackResponse {
    private String code;
    private String message;
    private String bookingCode;
    private String transactionNo;
}