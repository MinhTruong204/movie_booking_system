package com.viecinema.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeldSeatInfo {
    private Integer seatId;
    private String seatRow;
    private Integer seatNumber;
    private String seatType;
    private LocalDateTime heldUntil;
}
