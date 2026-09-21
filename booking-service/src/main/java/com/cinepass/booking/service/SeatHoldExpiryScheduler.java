package com.cinepass.booking.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinepass.booking.entity.BookingStatus;
import com.cinepass.booking.repository.BookingRepository;

@Component
public class SeatHoldExpiryScheduler {

    private final BookingRepository repository;
    private final BookingService service;

    public SeatHoldExpiryScheduler(BookingRepository repository, BookingService service) {
        this.repository = repository;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${booking.hold-expiry-sweep-fixed-delay-ms:15000}")
    public void expire() {
        repository.findByStatusAndHoldExpiresAtBefore(BookingStatus.HELD, LocalDateTime.now()).forEach(service::expireBooking);
    }
}
