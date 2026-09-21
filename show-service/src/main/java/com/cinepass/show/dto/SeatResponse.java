package com.cinepass.show.dto;

import java.math.BigDecimal;

public record SeatResponse(String seatNumber, String row, Integer column, String seatType, BigDecimal price) {

}
