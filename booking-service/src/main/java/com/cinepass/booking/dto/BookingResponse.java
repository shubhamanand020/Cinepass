package com.cinepass.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cinepass.booking.entity.BookingStatus;

public record BookingResponse(
        Long bookingId,
        Long showId,
        Long userId,
        List<String> seatNumbers,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime holdExpiresAt,
        LocalDateTime confirmedAt,
        String ticketToken,
        LocalDateTime checkedInAt
) {
    public BookingResponse(
            Long bookingId,
            Long showId,
            Long userId,
            List<String> seatNumbers,
            BigDecimal totalAmount,
            BookingStatus status,
            LocalDateTime createdAt,
            LocalDateTime holdExpiresAt,
            LocalDateTime confirmedAt
    ) {
        this(bookingId, showId, userId, seatNumbers, totalAmount, status, createdAt, holdExpiresAt, confirmedAt, null, null);
    }
}
