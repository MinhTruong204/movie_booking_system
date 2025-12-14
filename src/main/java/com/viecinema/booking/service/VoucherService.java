package com.viecinema.booking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class VoucherService {

    public BigDecimal calculateDiscount(
            String voucherCode,
            Integer userId,
            BigDecimal subtotal) {
        // TODO: Implement actual logic
        return BigDecimal.ZERO;
    }

    public void applyVoucher(
            String voucherCode,
            Integer userId,
            Integer bookingId,
            BigDecimal discountAmount) {
        // TODO: Save voucher usage
    }
}
