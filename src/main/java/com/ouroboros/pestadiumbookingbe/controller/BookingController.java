package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.BookingRequest;
import com.ouroboros.pestadiumbookingbe.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/create-booking")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest bookingRequest) {
        // Delegate to the service layer
        return bookingService.createBooking(
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getTimeSlotId(),
            bookingRequest.getPurpose()
        );
    }

    @PostMapping("/cancel-booking")
    public ResponseEntity<?> cancelBooking(@RequestParam UUID bookingId, @RequestParam UUID canceledBy) {
        return bookingService.cancelBooking(bookingId, canceledBy);
    }

    @PostMapping("/confirm-booking")
    public ResponseEntity<?> confirmBooking(@RequestParam UUID bookingId, @RequestParam UUID confirmedBy) {
        return bookingService.confirmBooking(bookingId, confirmedBy);
    }

    @PostMapping("/modify-booking")
    public ResponseEntity<?> modifyBooking(@RequestParam UUID bookingId, @RequestParam UUID modifiedByUserId, @RequestBody BookingRequest bookingRequest) {
        return bookingService.modifyBooking(
            bookingId,
            modifiedByUserId,
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getTimeSlotId(),
            bookingRequest.getPurpose()
        );
    }

    @GetMapping("/get-bookings")
    public ResponseEntity<?> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/get-booking/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping("/get-bookings-by-user/{userId}")
    public ResponseEntity<?> getBookingsByUserId(@PathVariable UUID userId) {
        return bookingService.getBookingsByUserId(userId);
    }
}
