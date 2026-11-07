package com.cinepass.user.dto;

public record AuthResponse(String token, Long userId, String email, String role) {
    public AuthResponse(String token, Long userId, String email) {
        this(token, userId, email, "ROLE_USER");
    }
}
