package com.viecinema.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PriceBreakdown {
    private BigDecimal ticketsSubtotal;    // Tổng tiền vé
    private BigDecimal combosSubtotal;     // Tổng tiền combo
    private BigDecimal subtotal;           // Tổng cộng trước giảm

    private BigDecimal promoDiscount;      // Giảm từ promo code
    private BigDecimal voucherDiscount;    // Giảm từ voucher
    private BigDecimal loyaltyDiscount;    // Giảm từ điểm
    private BigDecimal membershipDiscount; // Giảm từ hạng thành viên

    private BigDecimal totalDiscount;      // Tổng giảm
    private BigDecimal finalAmount;        // Số tiền phải trả

    // Điểm tích lũy nhận được
    private Integer pointsEarned;
}
