package com.viecinema.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PriceBreakdown {
    private BigDecimal ticketsSubtotal;
    private BigDecimal combosSubtotal;
    private BigDecimal subtotal;
    private BigDecimal promoDiscount;
    /** Giảm giá từ voucher TICKET_DISCOUNT (giảm tiền vé). */
    private BigDecimal ticketVoucherDiscount;
    /** Giảm giá từ voucher COMBO_DISCOUNT (miễn phí/giảm combo). */
    private BigDecimal comboVoucherDiscount;
    /** Tổng giảm từ voucher = ticketVoucherDiscount + comboVoucherDiscount. */
    private BigDecimal voucherDiscount;
    private BigDecimal loyaltyDiscount;
    private BigDecimal membershipDiscount;
    private BigDecimal totalDiscount;
    private BigDecimal finalAmount;
    private Integer pointsEarned;
}
