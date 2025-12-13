package com.viecinema.booking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateBookingRequest {

    @NotNull(message = "Showtime ID must not be null")
    private Integer showtimeId;

    @NotEmpty(message = "List of seat IDs must not be empty")
    @Size(max = 10, message = "List of seat IDs must not exceed 10 items")
    private List<Integer> seatIds;

    private List<ComboItem> combos;

    private String promotionCode;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItem {
        @NotNull
        private Integer comboId;

        @Min(value = 1, message = "The quantity must be at least 1")
        @Max(value = 20, message = "The quantity must not exceed 20")
        private Integer quantity;
    }
}
