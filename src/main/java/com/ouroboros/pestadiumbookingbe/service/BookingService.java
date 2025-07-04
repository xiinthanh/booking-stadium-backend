package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.*;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    @Autowired
    public ProfileRepository profileRepository;
    @Autowired
    public SportHallRepository sportHallRepository;
    @Autowired
    public TimeSlotRepository timeSlotRepository;

    // Rename validation methods to reflect inverted logic
    boolean isInvalidUser(UUID userId) {
        if (profileRepository.findById(userId).isEmpty()) {
            logger.warn("User profile not found for userId: {}", userId);
            return true;
        }
        return false;
    }

    boolean isInvalidSportHall(UUID sportHallId) {
        if (sportHallRepository.findById(sportHallId).isEmpty()) {
            logger.warn("Sport hall ID not found for sportHallId: {}", sportHallId);
            return true;
        }
        return false;
    }

    boolean isInvalidTimeSlot(UUID timeSlotId) {
        if (timeSlotRepository.findById(timeSlotId).isEmpty()) {
            logger.warn("Time slot ID not found for timeSlotId: {}", timeSlotId);
            return true;
        }
        return false;
    }

    boolean isInvalidBookingDate(LocalDate date) {
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

    boolean quotaExceeded(UUID userId, LocalDate date) {
        if (profileRepository.findById(userId).get().getType().equals(ProfileType.admin)) {
            return false;  // admin can have infinite amount of book
        }

        // Rule: A user can only have 1 booking/day
        long count = bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.confirmed)
                + bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.pending);

        if (count >= 1) {
            logger.warn("Quota exceeded for userId: {} on date: {}", userId, date);
            return true;
        }
        return false;
    }

    boolean isOccupiedBooking(UUID sportHallId, LocalDate date, UUID timeSlotId) {
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
    public Booking createBooking(UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Creating booking for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            // Validate input parameters
            if (isInvalidUser(userId) || isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) || isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters: userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", userId, sportHallId, sportId, date, timeSlotId, purpose);
                throw new BadRequestException("Invalid input parameters.");
            }

            // Check user's quota
            if (quotaExceeded(userId, date)) {
                throw new ForbiddenException("Quota exceeded for the user on the given date.");
            }

            // Make sure not overlapping with existing bookings
            if (isOccupiedBooking(sportHallId, date, timeSlotId)) {
                throw new ConflictException("A booking already exists for the given combination.");
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

            // Notify after transaction or immediately if no transaction active
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CREATION);
                }
            });

            return savedBooking;
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error during booking creation", ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
             logger.error("Transaction timed out during booking creation", ex);
             throw new RequestTimeoutException("The request timed out. Please try again later.");
        } catch (BadRequestException | ForbiddenException | ConflictException e) {
            throw e;
        } catch (Exception e) {
             logger.error("Error occurred while creating booking: {}", e.getMessage(), e);
        }
        throw new RuntimeException("Failed to create booking due to unexpected errors.");
    }

    // Ensure consistent usage of pessimistic locking in confirmBooking
    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public Booking confirmBooking(UUID bookingId, UUID confirmedBy) {
        logger.info("Confirming booking with ID: {} by user: {}", bookingId, confirmedBy);
        try {
            if (isInvalidUser(confirmedBy)) {
                throw new BadRequestException("User profile not found.");
            }

            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                throw new BadRequestException("Booking not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.rejected) {
                logger.error("Booking with ID: {} is not in pending/rejected status. Current status: {}", bookingId, booking.getStatus());
                throw new BadRequestException("Only pending/rejected bookings can be confirmed.");
            }

            if (booking.getStatus() == Status.rejected) {  // must check quota for confirming a rejected booking
                if (quotaExceeded(booking.getUserId(), booking.getBookingDate())) {
                    throw new ForbiddenException("Quota exceeded for the user on the booking date.");
                }
            }

            // Update booking status
            booking.setStatus(Status.confirmed);
            booking.setUpdatedAt(OffsetDateTime.now());
            booking.setCanceledAt(null);
            booking.setCanceledBy(null);
            Booking savedBooking = bookingRepository.save(booking);
            // Notify
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CONFIRMATION);
                }
            });

            return savedBooking;
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error during booking confirmation for booking ID: {}", bookingId, ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
             logger.error("Transaction timed out during booking confirmation for booking ID: {}", bookingId, ex);
             throw new RequestTimeoutException("The request timed out. Please try again later.");
        } catch (BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
             logger.error("Unexpected error confirming booking with ID: {}", bookingId, e);
             throw new RuntimeException("An unexpected error occurred while confirming the booking.");
        }
    }

    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public Booking cancelBooking(UUID bookingId, UUID canceledBy) {
        logger.info("Canceling booking with ID: {} by user: {}", bookingId, canceledBy);
        try {
            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                throw new BadRequestException("Booking not found.");
            }
            if (isInvalidUser(canceledBy)) {
                throw new BadRequestException("User profile not found.");
            }
            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                logger.error("Invalid booking status for cancellation: {}", booking.getStatus());
                throw new BadRequestException("Only pending/confirmed bookings can be canceled.");
            }
            booking.setCanceledAt(OffsetDateTime.now());
            booking.setCanceledBy(canceledBy);
            booking.setStatus(Status.rejected);
            Booking savedBooking = bookingRepository.save(booking);
            // Notify
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.CANCELLATION);
                }
            });

            logger.info("Booking canceled successfully with ID: {}", bookingId);
            return savedBooking;
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error during booking cancellation for booking ID: {}", bookingId, ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
             logger.error("Transaction timed out during booking cancellation for booking ID: {}", bookingId, ex);
             throw new RequestTimeoutException("The request timed out. Please try again later.");
        } catch (BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
             logger.error("Unexpected error canceling booking with ID: {}", bookingId, e);
             throw new RuntimeException("An unexpected error occurred while canceling the booking.");
        }
    }

    @Transactional(timeout = 2)  // 2 seconds timeout to prevent long-running transactions
    public Booking modifyBooking(UUID bookingId, UUID modifiedByUserId, UUID userId, UUID sportHallId, UUID sportId, LocalDate date, UUID timeSlotId, String purpose) {
        logger.info("Modifying booking with ID: {} for userId: {}, sportHallId: {}, sportId: {}, date: {}, timeSlotId: {}, purpose: {}", bookingId, userId, sportHallId, sportId, date, timeSlotId, purpose);
        try {
            if (bookingId == null || modifiedByUserId == null ||
                    isInvalidUser(modifiedByUserId) || isInvalidUser(userId) ||
                    isInvalidSportHall(sportHallId) || isInvalidTimeSlot(timeSlotId) ||
                    isInvalidBookingDate(date) || purpose == null || purpose.isEmpty()) {
                logger.error("Invalid input parameters for booking modification: bookingId={}, modifiedByUserId={}, userId={}, sportHallId={}, sportId={}, date={}, timeSlotId={}, purpose={}", bookingId, modifiedByUserId, userId, sportHallId, sportId, date, timeSlotId, purpose);
                throw new BadRequestException("Invalid input parameters.");
            }

            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                throw new BadRequestException("Booking not found.");
            }

            if (booking.getStatus() != Status.pending && booking.getStatus() != Status.confirmed) {
                logger.error("Invalid booking status for modification: {}", booking.getStatus());
                throw new BadRequestException("Only pending/confirmed bookings can be modified.");
            }

            // Quota check: if the booking date is changed, check if the user exceeds their quota on the new date
            if (!booking.getBookingDate().equals(date) && quotaExceeded(userId, date)) {
                throw new ForbiddenException("Quota exceeded for the user on the new date.");
            }

            // is occupied by another booking (not itself)
            if (!booking.getSportHallId().equals(sportHallId) ||
                    !booking.getBookingDate().equals(date) ||
                    !booking.getTimeSlotId().equals(timeSlotId)) {
                // the (sportHallId, bookingDate, getTimeSlotId) value of the new version does not match the older version
                if (isOccupiedBooking(sportHallId, date, timeSlotId)) {
                    throw new ConflictException("A booking already exists for the given combination.");
                }
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
            // Notify
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    notificationService.notifyOnBookingChange(savedBooking, BookingNotificationType.MODIFICATION);
                }
            });

            return savedBooking;
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error during booking modification", ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
             logger.error("Transaction timed out during booking modification for booking ID: {}", bookingId, ex);
             throw new RequestTimeoutException("The request timed out. Please try again later.");
        } catch (BadRequestException | ForbiddenException | ConflictException e) {
            throw e;
        } catch (Exception e) {
             logger.error("Error modifying booking with ID: {}", bookingId, e);
             throw new RuntimeException("An error occurred while modifying the booking.");
        }
    }

    @Transactional(timeout = 2)  // Ensure transaction is active for database operations
    public void deleteBooking(UUID bookingId, UUID deletedBy) {
        logger.info("Deleting booking with ID: {} by user: {}", bookingId, deletedBy);
        try {
            // Lock the booking to prevent concurrent modifications
            Booking booking = bookingRepository.findAndLockById(bookingId).orElse(null);
            if (booking == null) {
                logger.error("Booking not found with ID: {}", bookingId);
                throw new BadRequestException("Booking not found.");
            }

            if (isInvalidUser(deletedBy)) {
                throw new BadRequestException("User profile not found.");
            }

            bookingRepository.delete(booking);
            // Notify
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    notificationService.notifyOnBookingChange(booking, BookingNotificationType.DELETION);
                }
            });

            logger.info("Booking deleted successfully with ID: {}", bookingId);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error during booking deletion for booking ID: {}", bookingId, ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (TransactionTimedOutException ex) {
             logger.error("Transaction timed out during booking deletion for booking ID: {}", bookingId, ex);
             throw new RequestTimeoutException("The request timed out. Please try again later.");
        } catch (BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
             logger.error("Unexpected error deleting booking with ID: {}", bookingId, e);
             throw new RuntimeException("An unexpected error occurred while deleting the booking.");
        }
    }
}
