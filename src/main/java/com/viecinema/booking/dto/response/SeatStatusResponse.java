package com.viecinema.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusResponse {
    private Integer seatId;
    private String seatRow;
    private Integer seatNumber;
    private String status;
    private LocalDateTime heldUntil;
}
