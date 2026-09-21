package com.cinepass.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cinepass.booking.dto.BookingResponse;
import com.cinepass.booking.dto.HoldSeatsRequest;
import com.cinepass.booking.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse hold(Authentication auth, @Valid @RequestBody HoldSeatsRequest request) {
        return service.holdSeats(userId(auth), request);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(Authentication auth, @PathVariable Long id) {
        return service.confirm(userId(auth), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        return service.cancel(userId(auth), id);
    }

    @GetMapping("/{id}")
    public BookingResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(userId(auth), id);
    }

    @GetMapping
    public List<BookingResponse> mine(Authentication auth) {
        return service.byUser(userId(auth));
    }

    private Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
