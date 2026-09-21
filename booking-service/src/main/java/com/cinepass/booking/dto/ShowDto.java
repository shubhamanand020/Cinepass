package com.cinepass.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowDto(Long id, Long movieId, Long screenId, LocalDateTime showTime, BigDecimal basePrice, BigDecimal premiumPrice) {

}
