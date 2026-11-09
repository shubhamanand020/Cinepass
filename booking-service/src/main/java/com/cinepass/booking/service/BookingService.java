package com.cinepass.booking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinepass.booking.client.MovieServiceClient;
import com.cinepass.booking.client.ShowServiceClient;
import com.cinepass.booking.dto.BookingResponse;
import com.cinepass.booking.dto.HoldSeatsRequest;
import com.cinepass.booking.dto.MovieDto;
import com.cinepass.booking.dto.ScreenDto;
import com.cinepass.booking.dto.SeatAvailabilityResponse;
import com.cinepass.booking.dto.SeatDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.dto.TicketVerificationResponse;
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
    private final MovieServiceClient movieClient;
    private final QrCodeService qrCodeService;
    private final PdfTicketService pdfTicketService;
    @Value("${booking.hold-duration-seconds:300}")
    private long holdDurationSeconds;

    public BookingService(
            BookingRepository bookingRepository, 
            SeatAllocationRepository allocationRepository, 
            SeatAllocator seatAllocator, 
            ShowServiceClient showClient,
            MovieServiceClient movieClient,
            QrCodeService qrCodeService,
            PdfTicketService pdfTicketService
    ) {
        this.bookingRepository = bookingRepository;
        this.allocationRepository = allocationRepository;
        this.seatAllocator = seatAllocator;
        this.showClient = showClient;
        this.movieClient = movieClient;
        this.qrCodeService = qrCodeService;
        this.pdfTicketService = pdfTicketService;
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
        if (b.getTicketToken() == null) {
            b.setTicketToken(UUID.randomUUID().toString());
        }
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
    public List<BookingResponse> allBookings() {
        return bookingRepository.findAll().stream().map(this::response).toList();
    }

    @Transactional
    public BookingResponse adminCancel(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }
        allocationRepository.deleteByBookingId(id);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        return response(bookingRepository.save(booking));
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

    @Transactional(readOnly = true)
    public byte[] getQrCode(Long userId, Long id, boolean isAdmin) {
        Booking b = findBookingAuthorized(id, userId, isAdmin);
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("QR pass is only available for confirmed bookings");
        }
        String token = b.getTicketToken() != null ? b.getTicketToken() : ("BOOKING:" + b.getId());
        return qrCodeService.generateQrPng(token, 250, 250);
    }

    @Transactional(readOnly = true)
    public byte[] getPdfPass(Long userId, Long id, boolean isAdmin) {
        Booking b = findBookingAuthorized(id, userId, isAdmin);
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("PDF pass is only available for confirmed bookings");
        }

        ShowDto show = null;
        ScreenDto screen = null;
        MovieDto movie = null;
        try {
            show = showClient.getShow(b.getShowId());
            if (show != null) {
                if (show.screenId() != null) {
                    screen = showClient.getScreen(show.screenId());
                }
                if (show.movieId() != null) {
                    movie = movieClient.getMovie(show.movieId());
                }
            }
        } catch (Exception e) {
            // graceful fallback if remote service call fails
        }

        return pdfTicketService.generateTicketPdf(b, show, movie, screen);
    }

    @Transactional
    public TicketVerificationResponse verifyTicket(String ticketToken) {
        Booking b = bookingRepository.findByTicketToken(ticketToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or unknown ticket token: " + ticketToken));

        boolean isConfirmed = (b.getStatus() == BookingStatus.CONFIRMED);
        boolean wasAlreadyCheckedIn = (b.getCheckedInAt() != null);
        boolean valid = isConfirmed && !wasAlreadyCheckedIn;
        String message;

        if (!isConfirmed) {
            message = "Invalid admission: Ticket is " + b.getStatus().name();
        } else if (wasAlreadyCheckedIn) {
            message = "Warning: Ticket was already checked in on " + b.getCheckedInAt();
        } else {
            b.setCheckedInAt(LocalDateTime.now());
            b = bookingRepository.save(b);
            message = "Valid ticket. Check-in successful!";
        }

        ShowDto show = null;
        MovieDto movie = null;
        ScreenDto screen = null;
        try {
            show = showClient.getShow(b.getShowId());
            if (show != null) {
                if (show.movieId() != null) movie = movieClient.getMovie(show.movieId());
                if (show.screenId() != null) screen = showClient.getScreen(show.screenId());
            }
        } catch (Exception ignored) {}

        return new TicketVerificationResponse(
                valid,
                b.getId(),
                b.getTicketToken(),
                b.getShowId(),
                movie != null ? movie.title() : "Feature Film",
                screen != null ? screen.theatreName() : "Main Multiplex",
                screen != null ? screen.name() : ("Screen #" + b.getShowId()),
                show != null ? show.showTime() : null,
                new ArrayList<>(b.getSeatNumbers()),
                b.getCheckedInAt() != null,
                wasAlreadyCheckedIn,
                b.getCheckedInAt(),
                message
        );
    }

    private Booking findBookingAuthorized(Long id, Long userId, boolean isAdmin) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!isAdmin && !b.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + id);
        }
        return b;
    }

    private Booking owned(Long id, Long userId) {
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!b.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + id);
        
        }return b;
    }

    private BookingResponse response(Booking b) {
        return new BookingResponse(
                b.getId(), 
                b.getShowId(), 
                b.getUserId(), 
                new ArrayList<>(b.getSeatNumbers()), 
                b.getTotalAmount(), 
                b.getStatus(), 
                b.getCreatedAt(), 
                b.getHoldExpiresAt(), 
                b.getConfirmedAt(),
                b.getTicketToken(),
                b.getCheckedInAt()
        );
    }
}
