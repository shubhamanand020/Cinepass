package com.cinepass.show.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowResponse(Long id, Long movieId, Long screenId, LocalDateTime showTime, BigDecimal basePrice, BigDecimal premiumPrice) {

}
