package com.cinepass.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyTicketRequest {

    @NotBlank(message = "Ticket token is required")
    private String ticketToken;

    public VerifyTicketRequest() {
    }

    public VerifyTicketRequest(String ticketToken) {
        this.ticketToken = ticketToken;
    }

    public String getTicketToken() {
        return ticketToken;
    }

    public void setTicketToken(String ticketToken) {
        this.ticketToken = ticketToken;
    }
}

