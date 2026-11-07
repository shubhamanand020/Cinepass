package com.cinepass.booking.dto;

public record ScreenDto(
        Long id,
        String name,
        String theatreName,
        Integer rows,
        Integer seatsPerRow,
        Integer premiumRows
) {}

