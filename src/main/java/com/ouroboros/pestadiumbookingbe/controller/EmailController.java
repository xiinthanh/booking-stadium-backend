package com.ouroboros.pestadiumbookingbe.controller;


import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.BookingRequest;

import com.ouroboros.pestadiumbookingbe.util.BookingMapper;
import com.ouroboros.pestadiumbookingbe.util.EmailSender;
import com.ouroboros.pestadiumbookingbe.util.IcsFileGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailSender emailSender;
    private final IcsFileGenerator icsFileGenerator;
    private final BookingMapper bookingMapper;

    public EmailController(EmailSender emailSender, IcsFileGenerator icsFileGenerator, BookingMapper bookingMapper) {
        this.emailSender = emailSender;
        this.icsFileGenerator = icsFileGenerator;
        this.bookingMapper = bookingMapper;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestParam String to,
                                            @RequestParam String subject,
                                            @RequestParam String text) {
        try {
            emailSender.sendEmail(to, subject, text);
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email: " + e.getMessage());
        }
    }
    @PostMapping("/send-with-ics")
    public ResponseEntity<String> sendEmailWithIcs(@RequestParam String to,
                                                    @RequestParam String subject,
                                                    @RequestParam String text,
                                                    @ModelAttribute BookingRequest bookingRequest) throws Exception {
        try {
            Booking newBooking = new Booking();
            newBooking.setUserId(bookingRequest.getUserId());
            newBooking.setSportHallId(bookingRequest.getSportHallId());
            newBooking.setSportId(bookingRequest.getSportId());
            newBooking.setBookingDate(bookingRequest.getDate());
            newBooking.setTimeSlotId(bookingRequest.getTimeSlotId());
            newBooking.setPurpose(bookingRequest.getPurpose());

            BookingSummary bookingSummary = bookingMapper.toBookingSummary(newBooking);
            byte[] icsBytes = icsFileGenerator.generateIcsStream(bookingSummary).toByteArray();

            emailSender.sendEmailWithIcsAttachment(to, subject, text, icsBytes);
            return ResponseEntity.ok("Email with ICS sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email with ICS: " + e.getMessage());
        }
    }
}
