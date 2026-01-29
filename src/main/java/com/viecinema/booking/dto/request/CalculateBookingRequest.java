package com.viecinema.booking.dto.request;

import com.viecinema.booking.dto.SelectedCombo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private List<SelectedCombo> combos;

    private String promotionCode;
}
