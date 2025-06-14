package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    public ResponseEntity<?> createBooking(UUID userId, UUID sportHallId, LocalDate date, UUID timeSlotId, String purpose) {
        // Check if a booking with the same combination and status exists
        if (bookingRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                sportHallId, date, timeSlotId, Status.confirmed)) {
            return ResponseEntity.badRequest().body("A booking already exists for the given combination with a confirmed or completed status.");
        }

        // Create and save the new booking
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setSportHallId(sportHallId);
        booking.setBookingDate(date);
        booking.setTimeSlotId(timeSlotId);
        booking.setStatus(Status.pending);
        booking.setPurpose(purpose);

        booking.setCreatedAt(OffsetDateTime.now());
        booking.setUpdatedAt(OffsetDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);
        return ResponseEntity.ok(savedBooking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public ResponseEntity<?> cancelBooking(UUID bookingId, UUID canceledBy) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return ResponseEntity.badRequest().body("Booking not found.");
        }

        if (booking.getStatus() != Status.pending) {
            return ResponseEntity.badRequest().body("Only pending bookings can be canceled.");
        }

        booking.setCanceledAt(OffsetDateTime.now());
        booking.setCanceledBy(canceledBy);
        booking.setStatus(Status.rejected);

        Booking updatedBooking = bookingRepository.save(booking);
        return ResponseEntity.ok(updatedBooking);
    }

    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id).orElse(null);
    }
}
