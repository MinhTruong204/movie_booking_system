package com.viecinema.booking.dto;

import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateBookingResponse {

    private ShowtimeInfo showtime;
    private List<SeatInfo> seats;
    private List<ComboInfo> combos;
    private PricingBreakdown pricingBreakdown;

    @Getter
    @Setter
    @Builder
    public static class ComboInfo {
        private Integer comboId;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Getter
    @Setter
    @Builder
    public static class PricingBreakdown {
        private BigDecimal seatsSubtotal;
        private BigDecimal combosSubtotal;
        private BigDecimal subtotal;
        private PromotionInfo promotion;
        private BigDecimal totalDiscount;
        private BigDecimal finalAmount;
        private Integer loyaltyPointsEarned;
    }

    @Getter
    @Setter
    @Builder
    public static class PromotionInfo {
        private String code;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal discountAmount;
    }
}
