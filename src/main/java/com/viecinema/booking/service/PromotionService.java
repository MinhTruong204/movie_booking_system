package com.viecinema.booking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PromotionService {

    public BigDecimal calculateDiscount(
            String promoCode,
            Integer userId,
            BigDecimal subtotal,
            Integer showtimeId) {
        // TODO: Implement actual logic
        return BigDecimal. ZERO;
    }

    public void applyPromotion(
            String promoCode,
            Integer userId,
            Integer bookingId,
            BigDecimal discountAmount) {
        // TODO: Save promotion usage
    }
}
