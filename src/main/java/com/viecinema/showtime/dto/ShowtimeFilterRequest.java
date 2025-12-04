package com.viecinema.showtime.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeFilterRequest {

    @Min(value = 1, message = "Movie ID must be positive")
    private Integer movieId;

    @Min(value = 1, message = "Cinema ID must be positive")
    private Integer cinemaId;

    private Integer roomId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String city;
    private Boolean activeOnly = true;
    private Boolean futureOnly = true;
    private GroupBy groupBy = GroupBy.CINEMA;
    private SortBy sortBy = SortBy.START_TIME;
    private Boolean includeAvailableSeats = true;

    public enum GroupBy {
        CINEMA,      // Group theo rạp
        TIMESLOT,    // Group theo khung giờ (sáng, chiều, tối)
        ROOM,        // Group theo phòng chiếu
        NONE         // Không group, trả về flat list
    }

    public enum SortBy {
        START_TIME,     // Sắp xếp theo giờ chiếu
        PRICE,          // Sắp xếp theo giá
        CINEMA_NAME,    // Sắp xếp theo tên rạp
        AVAILABLE_SEATS // Sắp xếp theo số ghế trống
    }

    public boolean isValid() {
        return movieId != null || cinemaId != null;
    }

    public LocalDateTime getStartDateTime() {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return searchDate.atStartOfDay();
    }

    public LocalDateTime getEndDateTime() {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return searchDate.atTime(23, 59, 59);
    }
}
