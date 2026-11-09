package com.cinepass.booking.service;

import com.cinepass.booking.client.MovieServiceClient;
import com.cinepass.booking.client.ShowServiceClient;
import com.cinepass.booking.dto.MovieDto;
import com.cinepass.booking.dto.ScreenDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.dto.TicketVerificationResponse;
import com.cinepass.booking.entity.Booking;
import com.cinepass.booking.entity.BookingStatus;
import com.cinepass.booking.exception.ResourceNotFoundException;
import com.cinepass.booking.repository.BookingRepository;
import com.cinepass.booking.repository.SeatAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketVerificationRegressionTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SeatAllocationRepository allocationRepository;
    @Mock
    private SeatAllocator seatAllocator;
    @Mock
    private ShowServiceClient showClient;
    @Mock
    private MovieServiceClient movieClient;
    @Mock
    private QrCodeService qrCodeService;
    @Mock
    private PdfTicketService pdfTicketService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                allocationRepository,
                seatAllocator,
                showClient,
                movieClient,
                qrCodeService,
                pdfTicketService
        );
    }

    @Test
    @DisplayName("Valid confirmed ticket verifies successfully and marks checkedInAt")
    void testValidTicketVerification() {
        String token = "valid-token-123";
        Booking booking = new Booking();
        booking.setId(101L);
        booking.setUserId(2L);
        booking.setShowId(10L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTicketToken(token);
        booking.setSeatNumbers(List.of("A1", "A2"));
        booking.setCheckedInAt(null);

        when(bookingRepository.findByTicketToken(token)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        ShowDto show = new ShowDto(10L, 50L, 3L, LocalDateTime.now().plusHours(2), new BigDecimal("200"), new BigDecimal("350"));
        when(showClient.getShow(10L)).thenReturn(show);
        when(movieClient.getMovie(50L)).thenReturn(new MovieDto(50L, "Interstellar", "SciFi", "SciFi", "English", 169, "2024-11-05", "poster.jpg", "trailer.url"));
        when(showClient.getScreen(3L)).thenReturn(new ScreenDto(3L, "Audi 1 - IMAX", "PVR INOX", 10, 10, 2));

        TicketVerificationResponse response = bookingService.verifyTicket(token);

        assertNotNull(response);
        assertTrue(response.valid(), "Ticket must be marked valid on initial check-in");
        assertFalse(response.alreadyCheckedIn(), "Ticket must not be marked as already checked in prior");
        assertTrue(response.checkedIn(), "Ticket must be flagged checkedIn now");
        assertNotNull(response.checkedInAt(), "checkedInAt timestamp must be populated");
        assertEquals("Valid ticket. Check-in successful!", response.message());
        assertEquals("Interstellar", response.movieTitle());
        assertEquals("Audi 1 - IMAX", response.screenName());

        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("Duplicate verification of already checked-in ticket is rejected gracefully")
    void testAlreadyCheckedInTicketVerification() {
        String token = "already-checked-in-token-456";
        LocalDateTime priorCheckIn = LocalDateTime.now().minusMinutes(15);
        Booking booking = new Booking();
        booking.setId(102L);
        booking.setUserId(3L);
        booking.setShowId(10L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTicketToken(token);
        booking.setSeatNumbers(List.of("B3"));
        booking.setCheckedInAt(priorCheckIn);

        when(bookingRepository.findByTicketToken(token)).thenReturn(Optional.of(booking));

        TicketVerificationResponse response = bookingService.verifyTicket(token);

        assertNotNull(response);
        assertFalse(response.valid(), "Already checked-in ticket must not be admitted again");
        assertTrue(response.alreadyCheckedIn(), "Must be flagged as already checked in");
        assertTrue(response.checkedIn(), "checkedIn must remain true");
        assertEquals(priorCheckIn, response.checkedInAt(), "Timestamp must reflect prior check-in");
        assertTrue(response.message().contains("already checked in"), "Message must explain prior check-in");

        // Must NOT save booking again
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Invalid or non-existent ticket token throws ResourceNotFoundException")
    void testNonExistentTicketToken() {
        String token = "non-existent-token-999";
        when(bookingRepository.findByTicketToken(token)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.verifyTicket(token);
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancelled booking ticket verification is rejected")
    void testCancelledBookingVerification() {
        String token = "cancelled-ticket-token-789";
        Booking booking = new Booking();
        booking.setId(103L);
        booking.setUserId(4L);
        booking.setShowId(10L);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setTicketToken(token);
        booking.setSeatNumbers(List.of("C1"));
        booking.setCheckedInAt(null);

        when(bookingRepository.findByTicketToken(token)).thenReturn(Optional.of(booking));

        TicketVerificationResponse response = bookingService.verifyTicket(token);

        assertNotNull(response);
        assertFalse(response.valid(), "Cancelled ticket must be marked invalid");
        assertFalse(response.alreadyCheckedIn(), "Was not checked in");
        assertFalse(response.checkedIn(), "Cannot be checked in");
        assertTrue(response.message().contains("CANCELLED"));

        verify(bookingRepository, never()).save(any(Booking.class));
    }
}

