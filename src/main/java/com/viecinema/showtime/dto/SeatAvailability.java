package com.viecinema.showtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailability {
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer bookedSeats;
    private Integer heldSeats;
    private Double occupancyRate;

//    private Map<String, Integer> availableSeatsByType;
}
