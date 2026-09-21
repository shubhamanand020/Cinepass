package com.cinepass.booking.dto;

import java.math.BigDecimal;

public record SeatAvailabilityResponse(String seatNumber, String seatType, BigDecimal price, String status) {

}
