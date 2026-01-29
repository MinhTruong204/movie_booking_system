package com.viecinema.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HoldSeatsRequest {

    @NotNull(message = "Showtime ID cannot be null.")
    @Positive(message = "Showtime ID must be a positive number.")
    private Integer showtimeId;

    @NotNull(message = "List of seat IDs cannot be null.")
    @Size(min = 1, max = 10, message = "List of seat IDs must have at least 1 and at most 10 items")
    private List<@Positive(message = "Seat ID must be a positive number") Integer> seatIds;
}