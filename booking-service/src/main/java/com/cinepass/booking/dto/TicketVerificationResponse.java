package com.cinepass.booking.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TicketVerificationResponse(
        boolean valid,
        Long bookingId,
        String ticketToken,
        Long showId,
        String movieTitle,
        String theatreName,
        String screenName,
        LocalDateTime showTime,
        List<String> seatNumbers,
        boolean checkedIn,
        boolean alreadyCheckedIn,
        LocalDateTime checkedInAt,
        String message
) {}
