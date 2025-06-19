package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    public BookingRepository bookingRepository;
    @Autowired
    public NotificationService notificationService;
    @Autowired
    public ProfileRepository profileRepository;
    @Autowired
    public SportHallRepository sportHallRepository;
    @Autowired
    public TimeSlotRepository timeSlotRepository;

    // Rename validation methods to reflect inverted logic
    private boolean isInvalidUser(UUID userId) {
        return profileRepository.findById(userId).isEmpty();
    }

    private boolean isInvalidSportHall(UUID sportHallId) {
        return sportHallRepository.findById(sportHallId).isEmpty();
    }

    private boolean isInvalidTimeSlot(UUID timeSlotId) {
        return timeSlotRepository.findById(timeSlotId).isEmpty();
    }

    private boolean isInvalidBookingDate(LocalDate date) {
        return date.isBefore(LocalDate.now());
    }

    @Transactional
    public ResponseEntity<?> createBooking(UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Creating booking for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            // Validate input parameters
            if (isInvalidUser(userId) || isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) || isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters: userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", userId, sportHallId, sportId, date, timeSlotId, purpose);
                return ResponseEntity.badRequest().body("Invalid input parameters.");
            }

            // Use pessimistic locking to prevent race conditions
            // Lock rows with values (all rows with value (sportHallId, date, timeSlotId))
            Optional<Booking> existingBooking = bookingRepository.findBySportHallIdAndBookingDateAndTimeSlotId(sportHallId, date, timeSlotId);
            if (existingBooking.isPresent() && (existingBooking.get().getStatus() == Status.confirmed || existingBooking.get().getStatus() == Status.pending)) {
                logger.warn("Booking already exists for sportHallId: {}, date: {}, timeSlotId: {} with confirmed or pending status", sportHallId, date, timeSlotId);
                return ResponseEntity.badRequest().body("A booking already exists for the given combination.");
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
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            logger.error("Database constraint violation during booking creation", ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A booking already exists for the given combination.");
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking creation", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error occurred while creating booking: {}", e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create booking due to unexpected errors.");
    }

    // Ensure consistent usage of pessimistic locking in confirmBooking
    @Transactional
    public ResponseEntity<?> confirmBooking(UUID bookingId, UUID confirmedBy) {
        logger.info("Confirming booking with ID: {} by user: {}", bookingId, confirmedBy);
        try {
            if (isInvalidUser(confirmedBy)) {
                logger.error("User profile not found for userId: {}", confirmedBy);
                return ResponseEntity.badRequest().body("User profile not found.");
            }

            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.badRequest().body("Booking not found.");
            }

            if (booking.getStatus() != Status.pending) {
                logger.error("Booking with ID: {} is not in pending status. Current status: {}", bookingId, booking.getStatus());
                return ResponseEntity.badRequest().body("Only pending bookings can be confirmed.");
            }

            // Update booking status
            booking.setStatus(Status.confirmed);
            booking.setUpdatedAt(OffsetDateTime.now());
            booking.setCanceledAt(null);
            booking.setCanceledBy(null);
            Booking updatedBooking = bookingRepository.save(booking);

            // Lock the booking to prevent concurrent modifications
            // Reject all other bookings for the same sport hall, date, and time slot using a query
            bookingRepository.findBySportHallIdAndBookingDateAndTimeSlotId(booking.getSportHallId(), booking.getBookingDate(), booking.getTimeSlotId())
                .ifPresent(conflictingBooking -> {
                    if (!conflictingBooking.getId().equals(bookingId) &&
                        (conflictingBooking.getStatus() == Status.pending || conflictingBooking.getStatus() == Status.confirmed)) {
                        conflictingBooking.setStatus(Status.rejected);
                        conflictingBooking.setCanceledAt(OffsetDateTime.now());
                        conflictingBooking.setCanceledBy(confirmedBy);
                        bookingRepository.save(conflictingBooking);
                        logger.info("Rejected conflicting booking with ID: {} for sportHallId: {}, date: {}, timeSlotId: {}", conflictingBooking.getId(), conflictingBooking.getSportHallId(), conflictingBooking.getBookingDate(), conflictingBooking.getTimeSlotId());
                    }
                });

            logger.info("Booking confirmed successfully with ID: {}", bookingId);

            // Notify the user about the booking confirmation
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.CONFIRMATION);

            return ResponseEntity.ok(updatedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking confirmation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error confirming booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while confirming the booking.");
        }
    }

    @Transactional
    public ResponseEntity<?> cancelBooking(UUID bookingId, UUID canceledBy) {
        logger.info("Canceling booking with ID: {} by user: {}", bookingId, canceledBy);
        try {
            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.status(404).body("Booking not found.");
            }

            if (profileRepository.findById(canceledBy).isEmpty()) {
                logger.error("User profile not found for userId: {}", canceledBy);
                return ResponseEntity.badRequest().body("User profile not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                logger.error("Invalid booking status for cancellation: {}", booking.getStatus());
                return ResponseEntity.badRequest().body("Only pending/confirmed bookings can be canceled.");
            }

            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(canceledBy);
            booking.setStatus(Status.rejected);

            Booking updatedBooking = bookingRepository.save(booking);

            // Notify the user about the booking cancellation
            notificationService.notifyOnBookingChange(booking, BookingNotificationType.CANCELLATION);

            logger.info("Booking canceled successfully with ID: {}", bookingId);
            return ResponseEntity.ok(updatedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking cancellation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error canceling booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while canceling the booking.");
        }
    }

    @Transactional
    public ResponseEntity<?> modifyBooking(UUID bookingId, UUID modifiedByUserId, UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Modifying booking with ID: {} for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", bookingId, userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            if (bookingId == null || modifiedByUserId == null || isInvalidUser(userId) || isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) || isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters for booking modification: bookingId={}, modifiedByUserId={}, userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", bookingId, modifiedByUserId, userId, sportHallId, sportId, date, timeSlotId, purpose);
                return ResponseEntity.badRequest().body("Invalid input parameters.");
            }

            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.badRequest().body("Booking not found.");
            }

            // Lock the sport hall and time slot to prevent concurrent modifications
            bookingRepository.findBySportHallIdAndBookingDateAndTimeSlotId(
                    sportHallId, date, timeSlotId
            );

            // check if a booking with the same combination and status exists
            if (bookingRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                    sportHallId, date, timeSlotId, Status.confirmed) ||
                bookingRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                    sportHallId, date, timeSlotId, Status.pending)) {
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
            Optional<Booking> booking = bookingRepository.findById(id);
            if (booking.isPresent()) {
                return ResponseEntity.ok(booking.get());
            } else {
                logger.warn("No booking found for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking id not found.");
            }
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
            if (isInvalidUser(userId)) {
                logger.error("User profile not found for userId: {}", userId);
                return ResponseEntity.badRequest().body("User profile not found.");
            }

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
