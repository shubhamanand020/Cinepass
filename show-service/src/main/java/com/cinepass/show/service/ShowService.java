package com.cinepass.show.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cinepass.show.dto.CreateShowRequest;
import com.cinepass.show.dto.SeatResponse;
import com.cinepass.show.dto.ShowResponse;
import com.cinepass.show.entity.Screen;
import com.cinepass.show.entity.Show;
import com.cinepass.show.exception.ResourceNotFoundException;
import com.cinepass.show.repository.ShowRepository;

@Service
public class ShowService {

    private final ShowRepository repository;
    private final ScreenService screenService;

    public ShowService(ShowRepository repository, ScreenService screenService) {
        this.repository = repository;
        this.screenService = screenService;
    }

    public ShowResponse create(CreateShowRequest request) {
        screenService.getEntity(request.getScreenId());
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
        int premiumRows = Math.min(screen.getPremiumRows(), screen.getRows());
        return java.util.stream.IntStream.range(0, screen.getRows()).boxed()
                .flatMap(row -> java.util.stream.IntStream.rangeClosed(1, screen.getSeatsPerRow()).mapToObj(column -> {
            String rowLabel = String.valueOf((char) ('A' + row));
            boolean premium = row < premiumRows;
            BigDecimal price = premium ? show.getPremiumPrice() : show.getBasePrice();
            return new SeatResponse(rowLabel + column, rowLabel, column, premium ? "PREMIUM" : "STANDARD", price);
        })).sorted(Comparator.comparing(SeatResponse::row).thenComparing(SeatResponse::column)).toList();
    }

    private ShowResponse toResponse(Show s) {
        return new ShowResponse(s.getId(), s.getMovieId(), s.getScreenId(), s.getShowTime(), s.getBasePrice(), s.getPremiumPrice());
    }
}
