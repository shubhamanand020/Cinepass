package com.cinepass.show.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cinepass.show.dto.CreateScreenRequest;
import com.cinepass.show.dto.ScreenResponse;
import com.cinepass.show.entity.Screen;
import com.cinepass.show.exception.ResourceNotFoundException;
import com.cinepass.show.repository.ScreenRepository;

@Service
public class ScreenService {

    private final ScreenRepository repository;

    public ScreenService(ScreenRepository repository) {
        this.repository = repository;
    }

    public ScreenResponse create(CreateScreenRequest request) {
        Screen screen = new Screen();
        screen.setName(request.getName());
        screen.setTheatreName(request.getTheatreName());
        screen.setRows(request.getRows());
        screen.setSeatsPerRow(request.getSeatsPerRow());
        screen.setPremiumRows(request.getPremiumRows());
        return toResponse(repository.save(screen));
    }

    public List<ScreenResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public Screen getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + id));
    }

    public ScreenResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    private ScreenResponse toResponse(Screen s) {
        return new ScreenResponse(s.getId(), s.getName(), s.getTheatreName(), s.getRows(), s.getSeatsPerRow(), s.getPremiumRows());
    }
}
