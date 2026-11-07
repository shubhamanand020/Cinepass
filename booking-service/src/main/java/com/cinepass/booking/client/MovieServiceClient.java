package com.cinepass.booking.client;

import com.cinepass.booking.dto.MovieDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "movie-service")
public interface MovieServiceClient {
    @GetMapping("/api/movies/{id}")
    MovieDto getMovie(@PathVariable("id") Long id);
}

