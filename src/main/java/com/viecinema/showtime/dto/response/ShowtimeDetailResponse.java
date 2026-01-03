package com.viecinema.showtime.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.showtime.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDetailResponse {
    private Integer showtimeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal basePrice;
    private Boolean isActive;
    private MovieInfo movie;
    private CinemaInfo cinema;
    private RoomInfo room;
    private SeatAvailability seatAvailability;
    private PricingInfo pricing;
    private String timeSlot;
    private String format;

    public String calculateTimeSlot() {
        if (startTime == null) return null;

        int hour = startTime.getHour();
        if (hour >= 6 && hour < 12) {
            return "MORNING"; // 6h-12h
        } else if (hour >= 12 && hour < 18) {
            return "AFTERNOON"; // 12h-18h
        } else if (hour >= 18 && hour < 22) {
            return "EVENING"; // 18h-22h
        } else {
            return "NIGHT"; // 22h-6h
        }
    }
}
