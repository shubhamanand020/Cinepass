package com.cinepass.booking.service;

import com.cinepass.booking.dto.MovieDto;
import com.cinepass.booking.dto.ScreenDto;
import com.cinepass.booking.dto.ShowDto;
import com.cinepass.booking.entity.Booking;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfTicketService {

    private final QrCodeService qrCodeService;

    public PdfTicketService(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    public byte[] generateTicketPdf(Booking booking, ShowDto show, MovieDto movie, ScreenDto screen) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A5.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);

            document.open();

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(225, 29, 72)); // Rose 600
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(100, 116, 139));
            Font movieTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
            Font tokenFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 8, new Color(71, 85, 105));
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184));

            // Brand Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70, 30});

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            Paragraph brandTitle = new Paragraph("CINEPASS CINEMAS", brandFont);
            Paragraph brandSub = new Paragraph("OFFICIAL DIGITAL ADMISSION PASS", subtitleFont);
            brandCell.addElement(brandTitle);
            brandCell.addElement(brandSub);
            headerTable.addCell(brandCell);

            PdfPCell idCell = new PdfPCell();
            idCell.setBorder(Rectangle.NO_BORDER);
            idCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph bookingRef = new Paragraph("BOOKING REF: #" + booking.getId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(225, 29, 72)));
            bookingRef.setAlignment(Element.ALIGN_RIGHT);
            Paragraph statusText = new Paragraph("STATUS: " + booking.getStatus().name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 185, 129)));
            statusText.setAlignment(Element.ALIGN_RIGHT);
            idCell.addElement(bookingRef);
            idCell.addElement(statusText);
            headerTable.addCell(idCell);

            document.add(headerTable);

            // Divider Line
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingBefore(6);
            divider.setSpacingAfter(10);
            PdfPCell divCell = new PdfPCell();
            divCell.setBackgroundColor(new Color(225, 29, 72));
            divCell.setFixedHeight(2);
            divCell.setBorder(Rectangle.NO_BORDER);
            divider.addCell(divCell);
            document.add(divider);

            // Main Body: Left (Details) & Right (QR Pass)
            PdfPTable bodyTable = new PdfPTable(2);
            bodyTable.setWidthPercentage(100);
            bodyTable.setWidths(new float[]{65, 35});

            // Details Left Cell
            PdfPCell detailsCell = new PdfPCell();
            detailsCell.setBorder(Rectangle.NO_BORDER);
            detailsCell.setPaddingRight(12);

            String movieTitle = movie != null ? movie.title() : "Feature Film";
            Paragraph movieP = new Paragraph(movieTitle, movieTitleFont);
            detailsCell.addElement(movieP);

            if (movie != null) {
                Paragraph metaP = new Paragraph(movie.genre() + " • " + movie.language() + " • " + movie.duration() + " mins", subtitleFont);
                detailsCell.addElement(metaP);
            }

            // Show & Cinema Details Table
            PdfPTable metaGrid = new PdfPTable(2);
            metaGrid.setWidthPercentage(100);
            metaGrid.setSpacingBefore(10);
            metaGrid.setWidths(new float[]{50, 50});

            // Cinema & Screen
            addMetaItem(metaGrid, "THEATRE / HALL", screen != null ? screen.theatreName() : "Main Multiplex", labelFont, valueFont);
            addMetaItem(metaGrid, "SCREEN", screen != null ? screen.name() : (show != null ? ("Screen #" + show.screenId()) : "Screen #1"), labelFont, valueFont);

            // Showtime & Date
            String showDateFormatted = (show != null && show.showTime() != null) ? show.showTime().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) : "N/A";
            String showTimeFormatted = (show != null && show.showTime() != null) ? show.showTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "N/A";
            addMetaItem(metaGrid, "DATE", showDateFormatted, labelFont, valueFont);
            addMetaItem(metaGrid, "TIME", showTimeFormatted, labelFont, valueFont);

            // Seats & Amount
            String seatsStr = String.join(", ", booking.getSeatNumbers());
            addMetaItem(metaGrid, "SEATS (" + booking.getSeatNumbers().size() + ")", seatsStr, labelFont, valueFont);
            addMetaItem(metaGrid, "TOTAL PAID", "₹" + booking.getTotalAmount(), labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(225, 29, 72)));

            detailsCell.addElement(metaGrid);
            bodyTable.addCell(detailsCell);

            // QR Right Cell
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBackgroundColor(new Color(248, 250, 252)); // Slate 50
            qrCell.setBorderColor(new Color(226, 232, 240));
            qrCell.setBorderWidth(1);
            qrCell.setPadding(8);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Generate QR from ticket token
            String qrPayload = booking.getTicketToken() != null ? booking.getTicketToken() : ("BOOKING:" + booking.getId());
            byte[] qrBytes = qrCodeService.generateQrPng(qrPayload, 140, 140);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrCell.addElement(qrImage);

            Paragraph tokenLabel = new Paragraph("TICKET TOKEN", labelFont);
            tokenLabel.setAlignment(Element.ALIGN_CENTER);
            tokenLabel.setSpacingBefore(4);
            qrCell.addElement(tokenLabel);

            Paragraph tokenVal = new Paragraph(qrPayload, tokenFont);
            tokenVal.setAlignment(Element.ALIGN_CENTER);
            qrCell.addElement(tokenVal);

            Paragraph scanNote = new Paragraph("Present at gate for entry", noteFont);
            scanNote.setAlignment(Element.ALIGN_CENTER);
            scanNote.setSpacingBefore(4);
            qrCell.addElement(scanNote);

            bodyTable.addCell(qrCell);
            document.add(bodyTable);

            // Footer Note
            Paragraph footer = new Paragraph(
                    "This is an electronically generated valid cinema entry pass issued by Cinepass. Valid government ID may be requested upon entry.",
                    noteFont
            );
            footer.setSpacingBefore(14);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF ticket pass", e);
        }
    }

    private void addMetaItem(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }
}

