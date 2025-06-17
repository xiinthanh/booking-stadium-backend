package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    public ResponseEntity<?> createBooking(UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Creating booking for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", userId, sportHallId, sportId, date, timeSlotId, purpose);

        try {
            // Validate input parameters
            if (userId == null || sportHallId == null || sportId == null || date == null || timeSlotId == null || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters: userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", userId, sportHallId, sportId, date, timeSlotId, purpose);
                return ResponseEntity.badRequest().body("Invalid input parameters.");
            }

            if (date.isBefore(LocalDate.now())) {
                logger.error("Invalid booking date: {}. Date cannot be in the past.", date);
                return ResponseEntity.badRequest().body("Booking date cannot be in the past.");
            }

            // Check if a booking with the same combination and status exists
            if (bookingRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                    sportHallId, date, timeSlotId, Status.confirmed)) {
                logger.warn("Booking already exists for sportHallId: {}, date: {}, timeSlotId: {} with confirmed status", sportHallId, date, timeSlotId);
                return ResponseEntity.badRequest().body("A booking already exists for the given combination with a confirmed or completed status.");
            }

            // Create and save the new booking
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setSportHallId(sportHallId);
            booking.setSportId(sportId);
            booking.setBookingDate(date);
            booking.setTimeSlotId(timeSlotId);
            booking.setStatus(Status.pending);
            booking.setPurpose(purpose);
            booking.setCreatedAt(OffsetDateTime.now());
            booking.setUpdatedAt(OffsetDateTime.now());

            Booking savedBooking = bookingRepository.save(booking);
            logger.info("Booking created successfully for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}", userId, sportHallId, sportId, date, timeSlotId);
            return ResponseEntity.ok(savedBooking);
        } catch (Exception e) {
            logger.error("Error occurred while creating booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the booking.");
        }
    }

    public List<Booking> getAllBookings() {
        logger.info("Fetching all bookings");
        try {
            return bookingRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all bookings: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public ResponseEntity<?> cancelBooking(UUID bookingId, UUID canceledBy) {
        logger.info("Canceling booking with ID: {} by user: {}", bookingId, canceledBy);
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return ResponseEntity.badRequest().body("Booking not found.");
        }

        if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
            return ResponseEntity.badRequest().body("Only pending/confirmed bookings can be canceled.");
        }

        try {
            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(canceledBy);
            booking.setStatus(Status.rejected);

            Booking updatedBooking = bookingRepository.save(booking);
            return ResponseEntity.ok(updatedBooking);
        } catch (Exception e) {
            logger.error("Error canceling booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while canceling the booking.");
        }
    }

    public Booking getBookingById(UUID id) {
        logger.info("Fetching booking with bookingID: {}", id);
        try {
            return bookingRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching booking with ID: {}", id, e);
            return null;
        }
    }

    public List<Booking> getBookingsByUserId(UUID userId) {
        logger.info("Fetching bookings for userId: {}", userId);
        try {
            return bookingRepository.findAll().stream()
                    .filter(booking -> booking.getUserId().equals(userId))
                    .toList();
        } catch (Exception e) {
            logger.error("Error fetching bookings for userId: {}", userId, e);
            return List.of();
        }
    }

    public ResponseEntity<?> confirmBooking(UUID bookingId, UUID confirmedBy) {
        logger.info("Confirming booking with ID: {} by user: {}", bookingId, confirmedBy);
        try {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.badRequest().body("Booking not found.");
            }
            if (booking.getStatus() != Status.pending) {
                logger.error("Booking with ID: {} is not in pending status", bookingId);
                return ResponseEntity.badRequest().body("Only pending bookings can be confirmed.");
            }
            booking.setStatus(Status.confirmed);
            booking.setUpdatedAt(OffsetDateTime.now());
            booking.setCanceledAt(null);
            booking.setCanceledBy(null);
            Booking updatedBooking = bookingRepository.save(booking);

            // reject all other bookings for the same sport hall, date, and time slot
            try {
                bookingRepository.findAll().stream()
                        .filter(b -> b.getSportHallId().equals(booking.getSportHallId()) &&
                                     b.getBookingDate().equals(booking.getBookingDate()) &&
                                     b.getTimeSlotId().equals(booking.getTimeSlotId()) &&
                                     !b.getId().equals(bookingId) &&
                                     (b.getStatus() == Status.pending || b.getStatus() == Status.confirmed))
                        .forEach(b -> {
                            b.setStatus(Status.rejected);
                            b.setCanceledAt(OffsetDateTime.now());
                            b.setCanceledBy(confirmedBy);
                            bookingRepository.save(b);
                        });
            } catch (Exception e) {
                logger.error("Error rejecting conflicting bookings for booking ID: {}", bookingId, e);
            }

            logger.info("Booking confirmed successfully with ID: {}", bookingId);
            return ResponseEntity.ok(updatedBooking);
        } catch (Exception e) {
            logger.error("Error confirming booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while confirming the booking.");
        }
    }
}
