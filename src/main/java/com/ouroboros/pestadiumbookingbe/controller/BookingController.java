package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.config.UserPrincipal;
import com.ouroboros.pestadiumbookingbe.dto.BookingRequest;
import com.ouroboros.pestadiumbookingbe.exception.ForbiddenException;
import com.ouroboros.pestadiumbookingbe.service.BookingService;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.SportHallLocation;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private SearchService searchService;


    private boolean hasAdminRole(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    @PreAuthorize("hasRole('ADMIN') or principal.userId == #bookingRequest.userId")
    @PostMapping("/create-booking")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest bookingRequest) {
        Booking booking = bookingService.createBooking(
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getStartTime(),
            bookingRequest.getEndTime(),
            bookingRequest.getPurpose()
        );
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/cancel-booking")
    public ResponseEntity<Booking> cancelBooking(
            @RequestParam UUID bookingId,
            @RequestParam UUID canceledBy,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getUserId().equals(canceledBy)) {
            throw new ForbiddenException("Inconsistent user id.");
        }

        Booking booking = searchService.getBookingById(bookingId);
        if (!booking.getUserId().equals(principal.getUserId()) && !hasAdminRole(principal)) {
            throw new ForbiddenException("You can only cancel your own bookings.");
        }

        Booking updated = bookingService.cancelBooking(bookingId, canceledBy);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN') and principal.userId == #confirmedBy")
    @PostMapping("/confirm-booking")
    public ResponseEntity<Booking> confirmBooking(@RequestParam UUID bookingId, @RequestParam UUID confirmedBy) {
        Booking booking = bookingService.confirmBooking(bookingId, confirmedBy);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/modify-booking")
    public ResponseEntity<Booking> modifyBooking(
            @RequestParam UUID bookingId,
            @RequestParam UUID modifiedByUserId,
            @RequestBody BookingRequest bookingRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getUserId().equals(modifiedByUserId)) {
            throw new ForbiddenException("Inconsistent user id.");
        }

        Booking booking = searchService.getBookingById(bookingId);
        if (!booking.getUserId().equals(principal.getUserId()) && !hasAdminRole(principal)) {
            throw new ForbiddenException("You can only modify your own bookings.");
        }

        Booking updated = bookingService.modifyBooking(
            bookingId,
            modifiedByUserId,
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getSportId(),
            bookingRequest.getDate(),
            bookingRequest.getStartTime(),
            bookingRequest.getEndTime(),
            bookingRequest.getPurpose()
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/delete-booking")
    public ResponseEntity<Void> deleteBooking(
            @RequestParam UUID bookingId,
            @RequestParam UUID deletedBy,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getUserId().equals(deletedBy)) {
            throw new ForbiddenException("Inconsistent user id.");
        }

        Booking booking = searchService.getBookingById(bookingId);
        if (!booking.getUserId().equals(principal.getUserId()) && !hasAdminRole(principal)) {
            throw new ForbiddenException("You can only delete your own bookings.");
        }

        bookingService.deleteBooking(bookingId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = searchService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/get-booking/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable UUID id) {
        Booking booking = searchService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @PreAuthorize("hasRole('ADMIN') or principal.userId == #userId")
    @GetMapping("/get-bookings-by-user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUserId(@PathVariable UUID userId) {
        List<Booking> bookings = searchService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/filter-bookings")
    public ResponseEntity<List<Booking>> filterBookings(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) SportHallLocation location,
            @RequestParam(required = false) ProfileType profileType,
            @RequestParam(required = false) Status status) {
        try {
            List<Booking> bookings = searchService.filterBookings(
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
