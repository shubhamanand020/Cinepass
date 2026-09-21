package com.cinepass.show.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinepass.show.dto.CreateScreenRequest;
import com.cinepass.show.dto.ScreenResponse;
import com.cinepass.show.service.ScreenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/screens")
public class ScreenController {

    private final ScreenService service;

    public ScreenController(ScreenService service) {
        this.service = service;
    }

    @PostMapping
    public ScreenResponse create(@Valid @RequestBody CreateScreenRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ScreenResponse> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ScreenResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}
