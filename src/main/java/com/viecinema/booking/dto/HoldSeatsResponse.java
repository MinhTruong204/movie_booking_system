package com.viecinema.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsResponse {
    private Integer showtimeId;
    private List<HeldSeatDto> heldSeats;
    private LocalDateTime heldUntil;
    private Integer remainingSeconds;
}
