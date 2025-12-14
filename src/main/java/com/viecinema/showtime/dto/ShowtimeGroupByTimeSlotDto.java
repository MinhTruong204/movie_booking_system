package com.viecinema.showtime.dto;

import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeGroupByTimeSlotDto {

    private String timeSlot; // MORNING, AFTERNOON, EVENING, NIGHT
    private String timeSlotDisplay; // "Sáng (6h-12h)", "Chiều (12h-18h)"
    private String timeRange;

    private Integer totalShowtimes;
    private Integer totalAvailableSeats;

    private List<ShowtimeDetailResponse> showtimes;

    /**
     * Get display name cho time slot
     */
    public static String getTimeSlotDisplay(String timeSlot) {
        switch (timeSlot) {
            case "MORNING":
                return "Sáng (6h-12h)";
            case "AFTERNOON":
                return "Chiều (12h-18h)";
            case "EVENING":
                return "Tối (18h-22h)";
            case "NIGHT":
                return "Khuya (22h-6h)";
            default:
                return timeSlot;
        }
    }

    /**
     * Tính toán thống kê
     */
    public void calculateStats() {
        if (showtimes != null) {
            this.totalShowtimes = showtimes.size();
            this.totalAvailableSeats = showtimes.stream()
                    .mapToInt(st -> st.getSeatAvailability() != null ?
                            st. getSeatAvailability().getAvailableSeats() : 0)
                    .sum();
        }
        this.timeSlotDisplay = getTimeSlotDisplay(this.timeSlot);
    }
}
