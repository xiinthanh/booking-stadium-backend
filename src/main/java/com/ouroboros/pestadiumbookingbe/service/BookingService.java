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
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        if (profileRepository.findById(userId).isEmpty()) {
            logger.warn("User profile not found for userId: {}", userId);
            return true;
        }
        return false;
    }

    private boolean isInvalidSportHall(UUID sportHallId) {
        if (sportHallRepository.findById(sportHallId).isEmpty()) {
            logger.warn("Sport hall ID not found for sportHallId: {}", sportHallId);
            return true;
        }
        return false;
    }

    private boolean isInvalidTimeSlot(UUID timeSlotId) {
        if (timeSlotRepository.findById(timeSlotId).isEmpty()) {
            logger.warn("Time slot ID not found for timeSlotId: {}", timeSlotId);
            return true;
        }
        return false;
    }

    private boolean isInvalidBookingDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            logger.warn("Booking date cannot be in the past: {}", date);
            return true;
        }
        if (date.isAfter(LocalDate.now().plusYears(1))) {
            logger.warn("Booking date cannot be more than 1 year in the future: {}", date);
            return true;
        }
        return false;
    }

    private boolean quotaExceeded(UUID userId, LocalDate date) {
        // Rule: A user can only have 1 booking/day
        long count = bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.confirmed)
                + bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.pending);
        if (count >= 1) {
            logger.warn("Quota exceeded for userId: {} on date: {}", userId, date);
            return true;
        }
        return false;
    }

    private boolean isOccupiedBooking(UUID sportHallId, LocalDate date, UUID timeSlotId) {
        List<Booking> existingPendingBooking = bookingRepository.findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                sportHallId, date, timeSlotId, Status.pending);
        List<Booking> existingConfirmedBooking = bookingRepository.findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                sportHallId, date, timeSlotId, Status.confirmed);
        if (!existingPendingBooking.isEmpty() || !existingConfirmedBooking.isEmpty()) {
            logger.warn("A booking already exists for sportHallId: {}, date: {}, timeSlotId: {}", sportHallId, date, timeSlotId);
            return true;
        }
        return false;
    }

    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public ResponseEntity<?> createBooking(UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Creating booking for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            // Validate input parameters
            if (isInvalidUser(userId) || isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) || isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters: userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", userId, sportHallId, sportId, date, timeSlotId, purpose);
                return ResponseEntity.badRequest().body("Invalid input parameters.");
            }

            // Check user's quota
            if (quotaExceeded(userId, date)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Quota exceeded for the user on the given date.");
            }

            // Make sure not overlapping with existing bookings
            if (isOccupiedBooking(sportHallId, date, timeSlotId)) {
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
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CREATION);
                }
            });

            return ResponseEntity.ok(savedBooking);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            logger.error("Database constraint violation during booking creation", ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A booking already exists for the given combination.");
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking creation", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out during booking creation", ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("The request timed out. Please try again later.");
        } catch (Exception e) {
            logger.error("Error occurred while creating booking: {}", e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create booking due to unexpected errors.");
    }

    // Ensure consistent usage of pessimistic locking in confirmBooking
    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public ResponseEntity<?> confirmBooking(UUID bookingId, UUID confirmedBy) {
        logger.info("Confirming booking with ID: {} by user: {}", bookingId, confirmedBy);
        try {
            if (isInvalidUser(confirmedBy)) {
                return ResponseEntity.badRequest().body("User profile not found.");
            }

            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
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
            Booking savedBooking = bookingRepository.save(booking);

            // Lock the booking to prevent concurrent modifications
            // Make sure not overlapping with existing bookings
//            if (isOccupiedBooking(booking.getSportHallId(), booking.getBookingDate(), booking.getTimeSlotId())) {
//                return ResponseEntity.badRequest().body("A booking already exists for the given combination.");
//            }
            // Note: no need to check for overlapping bookings here as there is only 1 pending/confirmed
            // booking can exist at a time for a sport hall, date, and time slot.

            logger.info("Booking confirmed successfully with ID: {}", bookingId);

            // Notify the user about the booking confirmation
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CONFIRMATION);
                }
            });

            return ResponseEntity.ok(savedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking confirmation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out during booking confirmation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("The request timed out. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error confirming booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while confirming the booking.");
        }
    }

    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public ResponseEntity<?> cancelBooking(UUID bookingId, UUID canceledBy) {
        logger.info("Canceling booking with ID: {} by user: {}", bookingId, canceledBy);
        try {
            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.status(404).body("Booking not found.");
            }

            if (isInvalidUser(canceledBy)) {
                return ResponseEntity.badRequest().body("User profile not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                logger.error("Invalid booking status for cancellation: {}", booking.getStatus());
                return ResponseEntity.badRequest().body("Only pending/confirmed bookings can be canceled.");
            }

            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(canceledBy);
            booking.setStatus(Status.rejected);

            Booking savedBooking = bookingRepository.save(booking);

            // Notify the user about the booking cancellation
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CANCELLATION);
                }
            });

            logger.info("Booking canceled successfully with ID: {}", bookingId);
            return ResponseEntity.ok(savedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking cancellation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out during booking cancellation for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("The request timed out. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error canceling booking with ID: {}", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while canceling the booking.");
        }
    }

    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public ResponseEntity<?> modifyBooking(UUID bookingId, UUID modifiedByUserId, UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Modifying booking with ID: {} for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", bookingId, userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            if (bookingId == null || modifiedByUserId == null || isInvalidUser(userId) || isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) || isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters for booking modification: bookingId={}, modifiedByUserId={}, userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", bookingId, modifiedByUserId, userId, sportHallId, sportId, date, timeSlotId, purpose);
                return ResponseEntity.badRequest().body("Invalid input parameters.");
            }

            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                return ResponseEntity.badRequest().body("Booking not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                logger.error("Invalid booking status for modification: {}", booking.getStatus());
                return ResponseEntity.badRequest().body("Only pending/confirmed bookings can be modified.");
            }

            // Quota check: if the booking date is changed, check if the user exceeds their quota on the new date
            if (!booking.getBookingDate().equals(date) && quotaExceeded(userId, date)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Quota exceeded for the user on the new date.");
            }

            if (isOccupiedBooking(sportHallId, date, timeSlotId)) {
                return ResponseEntity.badRequest().body("A booking already exists for the given combination.");
            }

            // Update booking details
            booking.setSportHallId(sportHallId);
            booking.setSportId(sportId);
            booking.setUserId(userId);
            booking.setBookingDate(date);
            booking.setTimeSlotId(timeSlotId);
            booking.setPurpose(purpose);
            booking.setUpdatedAt(OffsetDateTime.now());
            booking.setCanceledAt(null);  // Clear cancellation details
            booking.setCanceledBy(null);  // Clear cancellation details
            booking.setStatus(Status.pending);  // Reset status to pending, waiting for confirmation from admin
            Booking savedBooking = bookingRepository.save(booking);

            // Notify the user about the booking modification
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.MODIFICATION);
                }
            });

            return ResponseEntity.ok(savedBooking);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking modification", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out during booking modification for booking ID: {}", bookingId, ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("The request timed out. Please try again later.");
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

            List<Booking> bookings = bookingRepository.findByUserId(userId);
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
