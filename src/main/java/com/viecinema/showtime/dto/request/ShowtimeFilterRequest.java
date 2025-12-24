package com.viecinema.showtime.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private LocalDateTime date;

    private String city;
    @Builder.Default
    private Boolean activeOnly = true;
    @Builder.Default
    private Boolean futureOnly = true;
    @Builder.Default
    private GroupBy groupBy = GroupBy.CINEMA;
    @Builder.Default
    private SortBy sortBy = SortBy.START_TIME;
    @Builder.Default
    private Boolean includeAvailableSeats = true;

    public enum GroupBy {
        CINEMA,
        TIMESLOT,
        ROOM,
        NONE
    }

    public enum SortBy {
        START_TIME,
        PRICE,
        CINEMA_NAME,
        AVAILABLE_SEATS
    }

    public boolean isValid() {
        return movieId != null || cinemaId != null;
    }
}
