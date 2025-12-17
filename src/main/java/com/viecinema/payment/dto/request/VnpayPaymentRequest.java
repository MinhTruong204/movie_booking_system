package com.viecinema.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentRequest {
    private Integer bookingId;
    private BigDecimal amount;
    private String orderInfo;  // Mô tả đơn hàng
    private String bankCode;   // Mã ngân hàng (optional, để user chọn)
    private String locale;     // vn hoặc en
}
