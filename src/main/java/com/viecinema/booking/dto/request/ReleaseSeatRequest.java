package com.viecinema.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReleaseSeatRequest {
    @NotNull
    private Integer showtimeId;
    @NotNull
    private Integer seatId;
    private Boolean force = false;
}
