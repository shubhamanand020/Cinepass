package com.cinepass.booking.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cinepass.booking.dto.BookingResponse;
import com.cinepass.booking.dto.HoldSeatsRequest;
import com.cinepass.booking.dto.TicketVerificationResponse;
import com.cinepass.booking.dto.VerifyTicketRequest;
import com.cinepass.booking.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse hold(Authentication auth, @Valid @RequestBody HoldSeatsRequest request) {
        return service.holdSeats(userId(auth), request);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(Authentication auth, @PathVariable Long id) {
        return service.confirm(userId(auth), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        return service.cancel(userId(auth), id);
    }

    @DeleteMapping("/{id}")
    public BookingResponse delete(Authentication auth, @PathVariable Long id) {
        return service.cancel(userId(auth), id);
    }

    @GetMapping("/{id}")
    public BookingResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(userId(auth), id);
    }

    @GetMapping
    public List<BookingResponse> mine(Authentication auth) {
        return service.byUser(userId(auth));
    }

    @GetMapping("/admin/all")
    public List<BookingResponse> allBookings() {
        return service.allBookings();
    }

    @PostMapping("/admin/{id}/cancel")
    public BookingResponse adminCancel(@PathVariable Long id) {
        return service.adminCancel(id);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQr(Authentication auth, @PathVariable Long id) {
        byte[] qr = service.getQrCode(userId(auth), id, isAdmin(auth));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qr);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(Authentication auth, @PathVariable Long id) {
        byte[] pdf = service.getPdfPass(userId(auth), id, isAdmin(auth));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Cinepass-Ticket-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/verify-ticket")
    public ResponseEntity<TicketVerificationResponse> verifyTicket(@Valid @RequestBody VerifyTicketRequest request) {
        return ResponseEntity.ok(service.verifyTicket(request.getTicketToken()));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
