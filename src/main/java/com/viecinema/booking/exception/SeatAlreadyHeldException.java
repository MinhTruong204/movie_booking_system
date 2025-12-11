package com.viecinema.booking.exception;

import com.viecinema.booking.dto.UnavailableSeatDto;
import lombok.Getter;

import java.util.List;

@Getter
public class SeatAlreadyHeldException extends RuntimeException {
    private final List<UnavailableSeatDto> unavailableSeats;
    private final List<Integer> availableSeats;

    public SeatAlreadyHeldException(
            String message,
            List<UnavailableSeatDto> unavailableSeats,
            List<Integer> availableSeats) {
        super(message);
        this.unavailableSeats = unavailableSeats;
        this.availableSeats = availableSeats;
    }
}