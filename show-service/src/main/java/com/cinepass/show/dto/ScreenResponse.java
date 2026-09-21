package com.cinepass.show.dto;

public record ScreenResponse(Long id, String name, String theatreName, Integer rows, Integer seatsPerRow, Integer premiumRows) {

}
