package com.viecinema.showtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingInfo {
    private BigDecimal basePrice;
    private Map<String, BigDecimal> pricesBySeatType; // Regular: 80000, VIP: 120000
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
