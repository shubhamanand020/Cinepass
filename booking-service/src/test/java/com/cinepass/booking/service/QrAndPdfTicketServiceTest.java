package com.cinepass.booking.service;

import com.cinepass.booking.dto.MovieDto;
import com.cinepass.booking.dto.ScreenDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.entity.Booking;
import com.cinepass.booking.entity.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QrAndPdfTicketServiceTest {

    private QrCodeService qrCodeService;
    private PdfTicketService pdfTicketService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
        pdfTicketService = new PdfTicketService(qrCodeService);
    }

    @Test
    @DisplayName("QrCodeService should generate valid PNG byte array with PNG signature")
    void testGenerateQrPng() {
        String testData = "CINEPASS:TICKET:test-uuid-12345";
        byte[] pngBytes = qrCodeService.generateQrPng(testData, 200, 200);

        assertNotNull(pngBytes, "PNG byte array must not be null");
        assertTrue(pngBytes.length > 50, "PNG byte array must contain image payload");

        // Verify standard PNG file signature: 0x89 0x50 0x4E 0x47 (\x89PNG)
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);
    }

    @Test
    @DisplayName("QrCodeService should generate valid Base64 Data URI")
    void testGenerateQrBase64() {
        String testData = "CINEPASS:TICKET:test-uuid-67890";
        String base64Uri = qrCodeService.generateQrBase64(testData, 150, 150);

        assertNotNull(base64Uri);
        assertTrue(base64Uri.startsWith("data:image/png;base64,"), "Data URI must start with PNG base64 prefix");
        assertTrue(base64Uri.length() > 100, "Base64 data URI should be properly populated");
    }

    @Test
    @DisplayName("PdfTicketService should generate valid PDF document bytes with %PDF signature")
    void testGenerateTicketPdf() {
        Booking booking = new Booking();
        booking.setId(101L);
        booking.setUserId(1L);
        booking.setShowId(202L);
        booking.setSeatNumbers(List.of("A1", "A2"));
        booking.setTotalAmount(new BigDecimal("700.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTicketToken("ticket-uuid-abc-123");
        booking.setCreatedAt(LocalDateTime.now());

        ShowDto show = new ShowDto(202L, 501L, 10L, LocalDateTime.now().plusDays(1), new BigDecimal("250.00"), new BigDecimal("350.00"));
        MovieDto movie = new MovieDto(501L, "Interstellar", "Space exploration", "Sci-Fi", "English", 169, "2024-11-05", "https://example.com/poster.jpg", "https://youtube.com/watch?v=abc");
        ScreenDto screen = new ScreenDto(10L, "Audi 1 - IMAX", "PVR ICON Luxe", 8, 10, 2);

        byte[] pdfBytes = pdfTicketService.generateTicketPdf(booking, show, movie, screen);

        assertNotNull(pdfBytes, "PDF bytes must not be null");
        assertTrue(pdfBytes.length > 500, "PDF document must be non-empty");

        // Verify standard PDF header "%PDF"
        assertEquals((byte) '%', pdfBytes[0]);
        assertEquals((byte) 'P', pdfBytes[1]);
        assertEquals((byte) 'D', pdfBytes[2]);
        assertEquals((byte) 'F', pdfBytes[3]);
    }

    @Test
    @DisplayName("PdfTicketService should handle null movie or screen gracefully with fallbacks")
    void testGenerateTicketPdfWithNullMetadata() {
        Booking booking = new Booking();
        booking.setId(102L);
        booking.setUserId(2L);
        booking.setShowId(203L);
        booking.setSeatNumbers(List.of("B5"));
        booking.setTotalAmount(new BigDecimal("250.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTicketToken(null); // will use fallback token
        booking.setCreatedAt(LocalDateTime.now());

        byte[] pdfBytes = pdfTicketService.generateTicketPdf(booking, null, null, null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals((byte) '%', pdfBytes[0]);
        assertEquals((byte) 'P', pdfBytes[1]);
        assertEquals((byte) 'D', pdfBytes[2]);
        assertEquals((byte) 'F', pdfBytes[3]);
    }
}

