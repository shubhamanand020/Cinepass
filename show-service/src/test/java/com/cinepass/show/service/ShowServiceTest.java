package com.cinepass.show.service;

import com.cinepass.show.dto.CreateShowRequest;
import com.cinepass.show.dto.SeatResponse;
import com.cinepass.show.dto.ShowResponse;
import com.cinepass.show.entity.Screen;
import com.cinepass.show.entity.Show;
import com.cinepass.show.exception.SchedulingConflictException;
import com.cinepass.show.repository.ShowRepository;
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
public class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ScreenService screenService;

    private ShowService showService;

    @BeforeEach
    void setUp() {
        showService = new ShowService(showRepository, screenService);
    }

    @Test
    @DisplayName("Dynamic Last-N Rows: Front rows must be STANDARD, only last N rows must be PREMIUM")
    void testDynamicLastNPremiumSeatAllocation() {
        // Setup Screen: 8 rows (A-H), 10 seats per row, 2 premium rows (G and H)
        Screen screen = new Screen();
        screen.setId(1L);
        screen.setName("Screen 1");
        screen.setRows(8);
        screen.setSeatsPerRow(10);
        screen.setPremiumRows(2);

        Show show = new Show();
        show.setId(10L);
        show.setScreenId(1L);
        show.setBasePrice(new BigDecimal("200.00"));
        show.setPremiumPrice(new BigDecimal("350.00"));

        when(showRepository.findById(10L)).thenReturn(Optional.of(show));
        when(screenService.getEntity(1L)).thenReturn(screen);

        List<SeatResponse> seats = showService.seats(10L);
        assertEquals(80, seats.size(), "Total seats should be 80 (8 rows x 10 seats)");

        // Rows A through F (indices 0 to 5) must be STANDARD at 200.00
        for (char row = 'A'; row <= 'F'; row++) {
            char r = row;
            List<SeatResponse> rowSeats = seats.stream().filter(s -> s.row().equals(String.valueOf(r))).toList();
            assertEquals(10, rowSeats.size());
            assertTrue(rowSeats.stream().allMatch(s -> s.seatType().equals("STANDARD")), "Row " + r + " must be STANDARD");
            assertTrue(rowSeats.stream().allMatch(s -> s.price().equals(new BigDecimal("200.00"))));
        }

        // Rows G and H (indices 6 and 7, the last 2 rows) must be PREMIUM at 350.00
        for (char row = 'G'; row <= 'H'; row++) {
            char r = row;
            List<SeatResponse> rowSeats = seats.stream().filter(s -> s.row().equals(String.valueOf(r))).toList();
            assertEquals(10, rowSeats.size());
            assertTrue(rowSeats.stream().allMatch(s -> s.seatType().equals("PREMIUM")), "Row " + r + " must be PREMIUM");
            assertTrue(rowSeats.stream().allMatch(s -> s.price().equals(new BigDecimal("350.00"))));
        }
    }

    @Test
    @DisplayName("Conflict Validation: Throws SchedulingConflictException when showtimes overlap on same screen")
    void testShowtimeConflictValidation_ThrowsExceptionOnOverlap() {
        Long screenId = 1L;
        LocalDateTime existingTime = LocalDateTime.of(2026, 12, 1, 18, 0); // 18:00 to 20:00

        Show existingShow = new Show();
        existingShow.setId(101L);
        existingShow.setScreenId(screenId);
        existingShow.setShowTime(existingTime);

        when(screenService.getEntity(screenId)).thenReturn(new Screen());
        when(showRepository.findByScreenId(screenId)).thenReturn(List.of(existingShow));

        // Attempt to create a show at 19:00 (overlaps with 18:00 - 20:00)
        CreateShowRequest request = new CreateShowRequest();
        request.setMovieId(2L);
        request.setScreenId(screenId);
        request.setShowTime(LocalDateTime.of(2026, 12, 1, 19, 0));
        request.setBasePrice(new BigDecimal("200.00"));
        request.setPremiumPrice(new BigDecimal("350.00"));

        assertThrows(SchedulingConflictException.class, () -> showService.create(request),
                "Should throw SchedulingConflictException when overlapping showtime is scheduled on same screen");
        verify(showRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conflict Validation: Allows non-overlapping showtimes on same screen or same time on different screens")
    void testShowtimeConflictValidation_AllowsNonOverlapping() {
        Long screenId = 1L;
        LocalDateTime existingTime = LocalDateTime.of(2026, 12, 1, 18, 0); // 18:00 to 20:00

        Show existingShow = new Show();
        existingShow.setId(101L);
        existingShow.setScreenId(screenId);
        existingShow.setShowTime(existingTime);

        when(screenService.getEntity(screenId)).thenReturn(new Screen());
        when(showRepository.findByScreenId(screenId)).thenReturn(List.of(existingShow));
        when(showRepository.save(any(Show.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Schedule at 20:30 (starts after 20:00, no overlap)
        CreateShowRequest validRequest = new CreateShowRequest();
        validRequest.setMovieId(2L);
        validRequest.setScreenId(screenId);
        validRequest.setShowTime(LocalDateTime.of(2026, 12, 1, 20, 30));
        validRequest.setBasePrice(new BigDecimal("200.00"));
        validRequest.setPremiumPrice(new BigDecimal("350.00"));

        ShowResponse response = showService.create(validRequest);
        assertNotNull(response);
        verify(showRepository, times(1)).save(any(Show.class));
    }
}
