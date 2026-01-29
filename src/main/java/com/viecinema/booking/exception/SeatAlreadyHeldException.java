package com.viecinema.booking.exception;

import com.viecinema.booking.dto.response.SeatStatusResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class SeatAlreadyHeldException extends RuntimeException {
    private final List<SeatStatusResponse> unavailableSeats;
    private final List<Integer> availableSeats;

    public SeatAlreadyHeldException(
            String message,
            List<SeatStatusResponse> unavailableSeats,
            List<Integer> availableSeats) {
        super(message);
        this.unavailableSeats = unavailableSeats;
        this.availableSeats = availableSeats;
    }
}