package com.viecinema.showtime.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeatSummary {
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer bookedSeats;
    private Integer heldSeats;
    private Integer disabledSeats;
    private Double occupancyRate; // % đã đặt
}