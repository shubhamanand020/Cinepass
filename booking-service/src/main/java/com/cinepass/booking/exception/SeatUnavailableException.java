package com.cinepass.booking.exception;

import java.util.List;

public class SeatUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> seats;

    public SeatUnavailableException(String message, List<String> seats) {
        super(message);
        this.seats = seats;
    }

    public List<String> getSeats() {
        return seats;
    }
}
