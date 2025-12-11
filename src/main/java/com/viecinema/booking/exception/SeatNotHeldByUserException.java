package com.viecinema.booking.exception;

public class SeatNotHeldByUserException extends RuntimeException {
    public SeatNotHeldByUserException(String message) {
        super(message);
    }
}
