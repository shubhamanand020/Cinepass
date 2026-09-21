package com.cinepass.booking.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinepass.booking.dto.SeatAvailabilityResponse;
import com.cinepass.booking.service.BookingService;

@RestController
@RequestMapping("/api/shows/{showId}/availability")
public class AvailabilityController {

    private final BookingService service;

    public AvailabilityController(BookingService service) {
        this.service = service;
    }

    @GetMapping
    public List<SeatAvailabilityResponse> availability(@PathVariable Long showId) {
        return service.availability(showId);
    }
}
