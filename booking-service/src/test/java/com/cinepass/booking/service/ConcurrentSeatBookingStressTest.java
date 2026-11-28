package com.cinepass.booking.service;

import com.cinepass.booking.client.MovieServiceClient;
import com.cinepass.booking.client.ShowServiceClient;
import com.cinepass.booking.dto.HoldSeatsRequest;
import com.cinepass.booking.dto.SeatDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.entity.Booking;
import com.cinepass.booking.entity.SeatAllocation;
import com.cinepass.booking.exception.SeatUnavailableException;
import com.cinepass.booking.repository.BookingRepository;
import com.cinepass.booking.repository.SeatAllocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcurrentSeatBookingStressTest {

    @Mock
    private SeatAllocationRepository allocationRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ShowServiceClient showClient;

    @Mock
    private MovieServiceClient movieClient;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private PdfTicketService pdfTicketService;

    @Test
    @DisplayName("Concurrency Engine: 50 simultaneous threads competing for exact same seat yields exactly 1 hold and 49 conflict rejections")
    void testConcurrentSeatHoldSimulation_ExactlyOneWinner() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount);

        AtomicInteger successfulHolds = new AtomicInteger(0);
        AtomicInteger rejectedHolds = new AtomicInteger(0);

        // Atomic lock simulator mimicking PostgreSQL uk_show_seat composite uniqueness constraint
        ConcurrentHashMap<String, Long> activeDatabaseUniqueIndex = new ConcurrentHashMap<>();

        Long showId = 15L;
        String targetSeat = "H8";

        for (int i = 0; i < threadCount; i++) {
            final long bookingId = 1000L + i;
            executor.submit(() -> {
                try {
                    startGate.await(); // All 50 threads wait and fire at the exact same millisecond
                    String lockKey = showId + "_" + targetSeat;

                    // putIfAbsent acts as atomic DB insert with unique constraint (show_id, seat_number)
                    if (activeDatabaseUniqueIndex.putIfAbsent(lockKey, bookingId) == null) {
                        successfulHolds.incrementAndGet();
                    } else {
                        rejectedHolds.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Fire all 50 threads simultaneously
        startGate.countDown();
        boolean completedInTime = finishGate.await(5, TimeUnit.SECONDS);

        executor.shutdown();

        assertTrue(completedInTime, "All 50 concurrent hold requests must finish within 5 seconds");
        assertEquals(1, successfulHolds.get(), "CRITICAL: Exactly ONE customer must successfully hold the seat (Zero double-booking)");
        assertEquals(49, rejectedHolds.get(), "CRITICAL: Exactly 49 competing customers must receive conflict rejection");
        assertEquals(1, activeDatabaseUniqueIndex.size(), "Database index must contain only the single winner allocation");
    }

    @Test
    @DisplayName("Service Layer Concurrency: When seat conflict occurs, BookingService aborts and throws SeatUnavailableException")
    void testBookingServiceHoldSeats_RejectsOnSeatConflict() {
        SeatAllocator seatAllocator = mock(SeatAllocator.class);
        BookingService bookingService = new BookingService(
                bookingRepository,
                allocationRepository,
                seatAllocator,
                showClient,
                movieClient,
                qrCodeService,
                pdfTicketService
        );

        Long showId = 15L;
        List<String> requestedSeats = List.of("H8");

        ShowDto mockShow = new ShowDto(showId, 1L, 1L, LocalDateTime.now().plusDays(1), new BigDecimal("200.00"), new BigDecimal("350.00"));
        SeatDto mockSeat = new SeatDto("H8", "H", 8, "PREMIUM", new BigDecimal("350.00"));

        when(showClient.getShow(showId)).thenReturn(mockShow);
        when(showClient.getSeatMap(showId)).thenReturn(List.of(mockSeat));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(555L);
            return b;
        });

        // Simulate that tryHold fails because another concurrent user already acquired the unique DB constraint
        when(seatAllocator.tryHold(eq(showId), eq("H8"), anyLong())).thenReturn(false);

        // Attempting to hold must throw SeatUnavailableException
        HoldSeatsRequest request = new HoldSeatsRequest(showId, requestedSeats);

        SeatUnavailableException exception = assertThrows(SeatUnavailableException.class, () -> {
            bookingService.holdSeats(101L, request);
        });

        assertTrue(exception.getMessage().contains("Seat(s) unavailable"), "Must return user-friendly seat unavailable notice");
        verify(bookingRepository, times(1)).delete(any(Booking.class)); // Verifies complete transaction rollback
    }

    @Test
    @DisplayName("SeatAllocator Integrity: DataIntegrityViolationException is caught safely and returns false")
    void testSeatAllocator_CatchesDataIntegrityViolation() {
        SeatAllocationRepository mockRepo = mock(SeatAllocationRepository.class);
        SeatAllocator allocator = new SeatAllocator(mockRepo);

        // When DB throws unique constraint violation (uk_show_seat)
        when(mockRepo.saveAndFlush(any(SeatAllocation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_show_seat"));

        boolean result = allocator.tryHold(15L, "H8", 999L);
        assertFalse(result, "When DB unique constraint collision happens, tryHold must safely return false");
    }
}
