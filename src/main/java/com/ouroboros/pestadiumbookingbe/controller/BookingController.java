package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.dto.BookingRequest;
import com.ouroboros.pestadiumbookingbe.dto.CancelRequest;
import com.ouroboros.pestadiumbookingbe.dto.ConfirmRequest;
import com.ouroboros.pestadiumbookingbe.dto.ModifyRequest;
import com.ouroboros.pestadiumbookingbe.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

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
    public ResponseEntity<?> cancelBooking(@RequestBody CancelRequest cancelRequest) {
        try {
            return bookingService.cancelBooking(cancelRequest.getBookingId(), cancelRequest.getCanceledBy());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid UUID format in request.");
        } catch (Exception e) {
            logger.error("Error occurred while canceling booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
        }
    }

    @PostMapping("/confirm-booking")
    public ResponseEntity<?> confirmBooking(@RequestBody ConfirmRequest confirmRequest) {
        try {
            return bookingService.confirmBooking(confirmRequest.getBookingId(), confirmRequest.getConfirmedBy());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid UUID format in request.");
        } catch (Exception e) {
            logger.error("Error occurred while confirming booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
        }
    }

    @PostMapping("/modify-booking")
    public ResponseEntity<?> modifyBooking(@RequestBody ModifyRequest modifyRequest) {
        return bookingService.modifyBooking(
            modifyRequest.getBookingId(),
            modifyRequest.getModifiedByUserId(),
            modifyRequest.getUserId(),
            modifyRequest.getSportHallId(),
            modifyRequest.getSportId(),
            modifyRequest.getDate(),
            modifyRequest.getTimeSlotId(),
            modifyRequest.getPurpose()
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
    public ResponseEntity<?> getBookingsByUserId(@PathVariable String userId) {
        // Validate userId format if necessary
        try {
            UUID userIdUUID = UUID.fromString(userId);
            return bookingService.getBookingsByUserId(userIdUUID);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid user ID format");
        }
    }
}

