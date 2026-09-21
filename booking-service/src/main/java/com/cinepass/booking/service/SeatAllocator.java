package com.cinepass.booking.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cinepass.booking.entity.SeatAllocation;
import com.cinepass.booking.entity.SeatStatus;
import com.cinepass.booking.repository.SeatAllocationRepository;

@Service
public class SeatAllocator {

    private final SeatAllocationRepository repository;

    public SeatAllocator(SeatAllocationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryHold(Long showId, String seatNumber, Long bookingId) {
        SeatAllocation allocation = new SeatAllocation();
        allocation.setShowId(showId);
        allocation.setSeatNumber(seatNumber);
        allocation.setBookingId(bookingId);
        allocation.setStatus(SeatStatus.HELD);
        allocation.setLockedAt(LocalDateTime.now());
        try {
            repository.saveAndFlush(allocation);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long showId, String seatNumber, Long bookingId) {
        repository.findByShowId(showId).stream().filter(a -> a.getBookingId().equals(bookingId) && a.getSeatNumber().equals(seatNumber)).forEach(repository::delete);
    }
}
