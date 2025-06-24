package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.dto.BookingRequest;
import com.ouroboros.pestadiumbookingbe.service.BookingService;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.SportHallLocation;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @PostMapping("/create-booking")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest bookingRequest) {
        Booking booking = bookingService.createBooking(
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getTimeSlotId(),
            bookingRequest.getPurpose()
        );
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/cancel-booking")
    public ResponseEntity<Booking> cancelBooking(@RequestParam UUID bookingId, @RequestParam UUID canceledBy) {
        Booking booking = bookingService.cancelBooking(bookingId, canceledBy);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/confirm-booking")
    public ResponseEntity<Booking> confirmBooking(@RequestParam UUID bookingId, @RequestParam UUID confirmedBy) {
        Booking booking = bookingService.confirmBooking(bookingId, confirmedBy);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/modify-booking")
    public ResponseEntity<Booking> modifyBooking(@RequestParam UUID bookingId, @RequestParam UUID modifiedByUserId, @RequestBody BookingRequest bookingRequest) {
        Booking booking = bookingService.modifyBooking(
            bookingId,
            modifiedByUserId,
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getTimeSlotId(),
            bookingRequest.getPurpose()
        );
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/delete-booking")
    public ResponseEntity<Void> deleteBooking(@RequestParam UUID bookingId, @RequestParam UUID deletedBy) {
        bookingService.deleteBooking(bookingId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/get-booking/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable UUID id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/get-bookings-by-user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUserId(@PathVariable String userId) {
        try {
            UUID userIdUUID = UUID.fromString(userId);
            List<Booking> bookings = bookingService.getBookingsByUserId(userIdUUID);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/filter-bookings")
    public ResponseEntity<List<Booking>> filterBookings(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) SportHallLocation location,
            @RequestParam(required = false) ProfileType profileType,
            @RequestParam(required = false) Status status) {
        try {
            List<Booking> bookings = bookingService.filterBookings(
                Optional.ofNullable(studentId),
                Optional.ofNullable(location),
                Optional.ofNullable(profileType),
                Optional.ofNullable(status)
            );
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
