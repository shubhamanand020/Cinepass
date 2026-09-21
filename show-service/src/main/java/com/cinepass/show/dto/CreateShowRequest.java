package com.cinepass.show.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateShowRequest {

    @NotNull
    private Long movieId;
    @NotNull
    private Long screenId;
    @NotNull
    private LocalDateTime showTime;
    @NotNull
    @Positive
    private BigDecimal basePrice;
    @NotNull
    @Positive
    private BigDecimal premiumPrice;

    public CreateShowRequest() {
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public LocalDateTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalDateTime showTime) {
        this.showTime = showTime;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getPremiumPrice() {
        return premiumPrice;
    }

    public void setPremiumPrice(BigDecimal premiumPrice) {
        this.premiumPrice = premiumPrice;
    }
}
