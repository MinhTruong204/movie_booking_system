package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SeatInfo {
    private Integer seatId;
    private String seatLabel;      // A1, A2, B1...
    private String rowLabel;       // A, B, C...
    private Integer seatNumber;    // 1, 2, 3...
    private Integer seatTypeId;
    private String seatTypeName;
    private BigDecimal priceMultiplier;
    private BigDecimal price;
    private String status;
    private Long holdExpiresIn;
    private Boolean isSelectable;
}

