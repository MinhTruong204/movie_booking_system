package com.viecinema.showtime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieInfo {
        private Integer movieId;
        private String title;
        private String posterUrl;
        private Integer duration;
        private String ageRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinemaInfo {
        private Integer cinemaId;
        private String name;
        private String address;
        private String city;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private Integer roomId;
        private String name;
        private Integer totalSeats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatAvailability {
        private Integer totalSeats;
        private Integer availableSeats;
        private Integer bookedSeats;
        private Integer heldSeats;
        private Double occupancyRate;

        // Số ghế available theo loại
        private Map<String, Integer> availableSeatsByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingInfo {
        private BigDecimal basePrice;
        private Map<String, BigDecimal> pricesBySeatType; // Regular: 80000, VIP: 120000
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
    }

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
