package com.cinepass.booking.exception;

public class InvalidBookingStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidBookingStateException(String message) {
        super(message);
    }
}
