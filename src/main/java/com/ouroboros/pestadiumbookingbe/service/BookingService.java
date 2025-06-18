package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    public BookingRepository bookingRepository;
    @Autowired
    public NotificationService notificationService;

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

            // Notify the user about the booking creation
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.CREATION);

            return ResponseEntity.ok(savedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking creation", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error occurred while creating booking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the booking.");
        }
    }

    public ResponseEntity<?> cancelBooking(UUID bookingId, UUID canceledBy) {
        logger.info("Canceling booking with ID: {} by user: {}", bookingId, canceledBy);
        try {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);

            if (booking == null) {
                return ResponseEntity.status(404).body("Booking not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                return ResponseEntity.badRequest().body("Only pending/confirmed bookings can be canceled.");
            }

            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(canceledBy);
            booking.setStatus(Status.rejected);

            Booking updatedBooking = bookingRepository.save(booking);

            // Notify the user about the booking cancellation
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.CANCELLATION);

            return ResponseEntity.ok(updatedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking cancellation", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error canceling booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while canceling the booking.");
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

            // Notify the user about the booking confirmation
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.CONFIRMATION);

            return ResponseEntity.ok(updatedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking confirmation", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error confirming booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while confirming the booking.");
        }
    }

    public ResponseEntity<?> modifyBooking(UUID bookingId, UUID modifiedByUserId, UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Modifying booking with ID: {} for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", bookingId, userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.badRequest().body("Booking not found.");
            }
            if (date.isBefore(LocalDate.now())) {
                logger.error("Invalid booking date: {}. Cannot modify booking in the past.", date);
                return ResponseEntity.badRequest().body("Booking date cannot be in the past.");
            }
            // check if a booking with the same combination and status exists
            if (bookingRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                    sportHallId, date, timeSlotId, Status.confirmed)) {
                logger.warn("Booking already exists for sportHallId: {}, date: {}, timeSlotId: {} with confirmed status", sportHallId, date, timeSlotId);
                return ResponseEntity.badRequest().body("A booking already exists for the given combination with a confirmed or completed status.");
            }
            // Update booking details
            booking.setStatus(Status.pending);  // Reset status to pending, waiting for confirmation from admin
            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(modifiedByUserId);
            bookingRepository.save(booking);

            // Notify the user about the booking modification
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.MODIFICATION);

            return ResponseEntity.ok("Booking modified successfully.");
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking modification", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error modifying booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while modifying the booking.");
        }
    }

    public ResponseEntity<?> getAllBookings() {
        logger.info("Fetching all bookings");
        try {
            return ResponseEntity.ok(bookingRepository.findAll());
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching all bookings", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching all bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    public ResponseEntity<?> getBookingById(UUID id) {
        logger.info("Fetching booking with bookingID: {}", id);
        try {
            return ResponseEntity.ok(bookingRepository.findById(id).orElse(null));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching booking with ID: {}", id, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        } catch (Exception e) {
            logger.error("Error fetching booking with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<?> getBookingsByUserId(UUID userId) {
        logger.info("Fetching bookings for userId: {}", userId);
        try {
            List<Booking> bookings = bookingRepository.findAll().stream()
                    .filter(booking -> booking.getUserId().equals(userId))
                    .toList();
            return ResponseEntity.ok(bookings);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching bookings for userId: {}", userId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching bookings for userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
