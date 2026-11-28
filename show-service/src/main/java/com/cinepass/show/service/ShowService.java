package com.cinepass.show.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cinepass.show.dto.CreateShowRequest;
import com.cinepass.show.dto.SeatResponse;
import com.cinepass.show.dto.ShowResponse;
import com.cinepass.show.entity.Screen;
import com.cinepass.show.entity.Show;
import com.cinepass.show.exception.ResourceNotFoundException;
import com.cinepass.show.exception.SchedulingConflictException;
import com.cinepass.show.repository.ShowRepository;

@Service
public class ShowService {

    public static final int DEFAULT_SHOW_DURATION_MINUTES = 120; // Standard 2-hour theatrical slot buffer

    private final ShowRepository repository;
    private final ScreenService screenService;

    public ShowService(ShowRepository repository, ScreenService screenService) {
        this.repository = repository;
        this.screenService = screenService;
    }

    public ShowResponse create(CreateShowRequest request) {
        screenService.getEntity(request.getScreenId());
        validateShowtimeConflict(request.getScreenId(), request.getShowTime(), null);
        Show show = new Show();
        show.setMovieId(request.getMovieId());
        show.setScreenId(request.getScreenId());
        show.setShowTime(request.getShowTime());
        show.setBasePrice(request.getBasePrice());
        show.setPremiumPrice(request.getPremiumPrice());
        return toResponse(repository.save(show));
    }

    public List<ShowResponse> findAll(Long movieId) {
        List<Show> shows = movieId == null ? repository.findAll() : repository.findByMovieId(movieId);
        return shows.stream().map(this::toResponse).toList();
    }

    public Show getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
    }

    public ShowResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    public List<SeatResponse> seats(Long showId) {
        Show show = getEntity(showId);
        Screen screen = screenService.getEntity(show.getScreenId());
        int totalRows = screen.getRows();
        int premiumRows = Math.max(0, Math.min(screen.getPremiumRows() != null ? screen.getPremiumRows() : 0, totalRows));
        int premiumStartRow = totalRows - premiumRows;
        return java.util.stream.IntStream.range(0, totalRows).boxed()
                .flatMap(row -> java.util.stream.IntStream.rangeClosed(1, screen.getSeatsPerRow()).mapToObj(column -> {
            String rowLabel = String.valueOf((char) ('A' + row));
            boolean premium = premiumRows > 0 && row >= premiumStartRow;
            BigDecimal price = premium ? show.getPremiumPrice() : show.getBasePrice();
            return new SeatResponse(rowLabel + column, rowLabel, column, premium ? "PREMIUM" : "STANDARD", price);
        })).sorted(Comparator.comparing(SeatResponse::row).thenComparing(SeatResponse::column)).toList();
    }

    public ShowResponse update(Long id, CreateShowRequest request) {
        Show show = getEntity(id);
        screenService.getEntity(request.getScreenId());
        validateShowtimeConflict(request.getScreenId(), request.getShowTime(), id);
        show.setMovieId(request.getMovieId());
        show.setScreenId(request.getScreenId());
        show.setShowTime(request.getShowTime());
        show.setBasePrice(request.getBasePrice());
        show.setPremiumPrice(request.getPremiumPrice());
        return toResponse(repository.save(show));
    }

    public void delete(Long id) {
        Show show = getEntity(id);
        repository.delete(show);
    }

    private void validateShowtimeConflict(Long screenId, LocalDateTime candidateStart, Long excludeShowId) {
        if (screenId == null || candidateStart == null) return;
        LocalDateTime candidateEnd = candidateStart.plusMinutes(DEFAULT_SHOW_DURATION_MINUTES);

        List<Show> existingShows = repository.findByScreenId(screenId);
        for (Show existing : existingShows) {
            if (excludeShowId != null && existing.getId().equals(excludeShowId)) {
                continue;
            }
            if (existing.getShowTime() == null) continue;

            LocalDateTime existingStart = existing.getShowTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(DEFAULT_SHOW_DURATION_MINUTES);

            // Overlap check: candidate starts before existing ends AND candidate ends after existing starts
            if (candidateStart.isBefore(existingEnd) && candidateEnd.isAfter(existingStart)) {
                throw new SchedulingConflictException(String.format(
                    "Scheduling conflict on Screen #%d: Another movie showtime is already running from %s to %s. Overlapping schedules are not permitted on the same screen.",
                    screenId, existingStart, existingEnd
                ));
            }
        }
    }

    private ShowResponse toResponse(Show s) {
        return new ShowResponse(s.getId(), s.getMovieId(), s.getScreenId(), s.getShowTime(), s.getBasePrice(), s.getPremiumPrice());
    }
}
