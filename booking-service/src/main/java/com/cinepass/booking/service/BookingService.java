package com.cinepass.booking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinepass.booking.client.ShowServiceClient;
import com.cinepass.booking.dto.BookingResponse;
import com.cinepass.booking.dto.HoldSeatsRequest;
import com.cinepass.booking.dto.SeatAvailabilityResponse;
import com.cinepass.booking.dto.SeatDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.entity.Booking;
import com.cinepass.booking.entity.BookingStatus;
import com.cinepass.booking.entity.SeatAllocation;
import com.cinepass.booking.entity.SeatStatus;
import com.cinepass.booking.exception.InvalidBookingStateException;
import com.cinepass.booking.exception.ResourceNotFoundException;
import com.cinepass.booking.exception.SeatUnavailableException;
import com.cinepass.booking.repository.BookingRepository;
import com.cinepass.booking.repository.SeatAllocationRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatAllocationRepository allocationRepository;
    private final SeatAllocator seatAllocator;
    private final ShowServiceClient showClient;
    @Value("${booking.hold-duration-seconds:300}")
    private long holdDurationSeconds;

    public BookingService(BookingRepository bookingRepository, SeatAllocationRepository allocationRepository, SeatAllocator seatAllocator, ShowServiceClient showClient) {
        this.bookingRepository = bookingRepository;
        this.allocationRepository = allocationRepository;
        this.seatAllocator = seatAllocator;
        this.showClient = showClient;
    }

    @Transactional
    public BookingResponse holdSeats(Long userId, HoldSeatsRequest request) {
        ShowDto show = showClient.getShow(request.getShowId());
        Map<String, SeatDto> seats = showClient.getSeatMap(request.getShowId()).stream().collect(Collectors.toMap(SeatDto::seatNumber, s -> s));
        List<String> requested = request.getSeatNumbers().stream().distinct().toList();
        List<String> unknown = requested.stream().filter(s -> !seats.containsKey(s)).toList();
        if (!unknown.isEmpty()) {
            throw new ResourceNotFoundException("Unknown seat(s): " + unknown);
        }
        BigDecimal total = requested.stream().map(s -> seats.get(s).price()).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setShowId(show.id());
        booking.setUserId(userId);
        booking.setSeatNumbers(new ArrayList<>(requested));
        booking.setTotalAmount(total);
        booking.setStatus(BookingStatus.HELD);
        booking.setCreatedAt(now);
        booking.setHoldExpiresAt(now.plusSeconds(holdDurationSeconds));
        booking = bookingRepository.save(booking);
        List<String> claimed = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        for (String seat : requested) {
            if (seatAllocator.tryHold(show.id(), seat, booking.getId())) {
                claimed.add(seat);
            } else {
                conflicts.add(seat);
        
            }}
        if (!conflicts.isEmpty()) {
            for (String seat : claimed) {
                seatAllocator.release(show.id(), seat, booking.getId());
            }
            bookingRepository.delete(booking);
            throw new SeatUnavailableException("Seat(s) unavailable: " + conflicts, conflicts);
        }
        return response(booking);
    }

    @Transactional
    public BookingResponse confirm(Long userId, Long id) {
        Booking b = owned(id, userId);
        if (b.getStatus() != BookingStatus.HELD) {
            throw new InvalidBookingStateException("Booking is not held");
        
        }if (expired(b)) {
            expireBooking(b);
            throw new InvalidBookingStateException("Booking hold has expired");
        }
        allocationRepository.findByBookingId(id).forEach(a -> a.setStatus(SeatStatus.CONFIRMED));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setConfirmedAt(LocalDateTime.now());
        return response(bookingRepository.save(b));
    }

    @Transactional
    public BookingResponse cancel(Long userId, Long id) {
        Booking b = owned(id, userId);
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidBookingStateException("Booking is already closed");
        
        }allocationRepository.deleteByBookingId(id);
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(LocalDateTime.now());
        return response(bookingRepository.save(b));
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long userId, Long id) {
        return response(owned(id, userId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> byUser(Long userId) {
        return bookingRepository.findByUserId(userId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> availability(Long showId) {
        Map<String, SeatAllocation> allocated = allocationRepository.findByShowId(showId).stream().collect(Collectors.toMap(SeatAllocation::getSeatNumber, a -> a));
        return showClient.getSeatMap(showId).stream().map(s -> new SeatAvailabilityResponse(s.seatNumber(), s.seatType(), s.price(), allocated.containsKey(s.seatNumber()) ? allocated.get(s.seatNumber()).getStatus().name() : "AVAILABLE")).toList();
    }

    @Transactional
    public void expireBooking(Booking b) {
        allocationRepository.deleteByBookingId(b.getId());
        b.setStatus(BookingStatus.EXPIRED);
        b.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(b);
    }

    private boolean expired(Booking b) {
        return b.getHoldExpiresAt() != null && b.getHoldExpiresAt().isBefore(LocalDateTime.now());
    }

    private Booking owned(Long id, Long userId) {
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!b.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + id);
        
        }return b;
    }

    private BookingResponse response(Booking b) {
        return new BookingResponse(b.getId(), b.getShowId(), b.getUserId(), new ArrayList<>(b.getSeatNumbers()), b.getTotalAmount(), b.getStatus(), b.getCreatedAt(), b.getHoldExpiresAt(), b.getConfirmedAt());
    }
}
