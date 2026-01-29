package com.viecinema.booking.exception;

import com.viecinema.booking.dto.response.SeatStatusResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class SeatAlreadyBookedException extends RuntimeException {
    private final List<SeatStatusResponse> bookedSeats;

    public SeatAlreadyBookedException(String message, List<SeatStatusResponse> bookedSeats) {
        super(message);
        this.bookedSeats = bookedSeats;
    }
}
