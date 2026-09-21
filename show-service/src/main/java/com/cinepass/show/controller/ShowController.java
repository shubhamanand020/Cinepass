package com.cinepass.show.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinepass.show.dto.CreateShowRequest;
import com.cinepass.show.dto.SeatResponse;
import com.cinepass.show.dto.ShowResponse;
import com.cinepass.show.service.ShowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService service;

    public ShowController(ShowService service) {
        this.service = service;
    }

    @PostMapping
    public ShowResponse create(@Valid @RequestBody CreateShowRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ShowResponse> all(@RequestParam(required = false) Long movieId) {
        return service.findAll(movieId);
    }

    @GetMapping("/{id}")
    public ShowResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return service.seats(id);
    }
}
