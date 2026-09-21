package com.cinepass.booking.client;

import com.cinepass.booking.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "show-service")
public interface ShowServiceClient {
    @GetMapping("/api/shows/{id}") ShowDto getShow(@PathVariable("id") Long id);
    @GetMapping("/api/shows/{id}/seats") List<SeatDto> getSeatMap(@PathVariable("id") Long id);
}
