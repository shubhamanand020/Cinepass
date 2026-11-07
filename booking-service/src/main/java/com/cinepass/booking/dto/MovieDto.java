package com.cinepass.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieDto(
        Long id,
        String title,
        String description,
        String genre,
        String language,
        Integer duration,
        String releaseDate,
        String posterUrl,
        String trailerUrl
) {}

