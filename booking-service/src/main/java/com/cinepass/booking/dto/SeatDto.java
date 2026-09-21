package com.cinepass.booking.dto;

import java.math.BigDecimal;

public record SeatDto(String seatNumber, String row, Integer column, String seatType, BigDecimal price) {

}
