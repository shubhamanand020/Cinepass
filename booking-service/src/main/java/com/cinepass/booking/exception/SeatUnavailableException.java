package com.cinepass.booking.exception;

import java.util.List;

public class SeatUnavailableException extends RuntimeException {

    private final List<String> seats;

    public SeatUnavailableException(String message, List<String> seats) {
        super(message);
        this.seats = seats;
    }

    public List<String> getSeats() {
        return seats;
    }
}
