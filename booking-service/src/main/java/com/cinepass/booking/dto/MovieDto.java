package com.cinepass.booking.dto;

public record MovieDto(
        Long id,
        String title,
        String description,
        String genre,
        String language,
        Integer duration,
        String releaseDate,
        String posterUrl
) {}

