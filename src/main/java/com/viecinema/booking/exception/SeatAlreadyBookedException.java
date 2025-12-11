package com.viecinema.booking.exception;

import com.viecinema.booking.dto.UnavailableSeatDto;
import lombok.Getter;

import java.util.List;

@Getter
public class SeatAlreadyBookedException extends RuntimeException {
    private final List<UnavailableSeatDto> bookedSeats;

    public SeatAlreadyBookedException(String message, List<UnavailableSeatDto> bookedSeats) {
        super(message);
        this.bookedSeats = bookedSeats;
    }
}
