package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatTypeInfo {
    private Integer seatTypeId;
    private String name;
    private String description;
    private BigDecimal priceMultiplier;
    private BigDecimal finalPrice; // basePrice * priceMultiplier
    private String colorCode;      // Màu hiển thị trên UI
    private Integer availableCount;
}
