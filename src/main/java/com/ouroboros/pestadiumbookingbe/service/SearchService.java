package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.exception.RequestTimeoutException;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ProfileRepository profileRepository;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SearchService.class);

    public SportHall getSportHallById(UUID id) {
        logger.info("Fetching sport hall with ID: {}", id);
        try {
            Optional<SportHall> foundSportHall = sportHallRepository.findById(id);
            return foundSportHall
                    .orElseThrow(() -> new BadRequestException("Sport hall not found"));
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching sport hall with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching sport hall with ID: {}", id, e);
            throw new RuntimeException("Unexpected error fetching sport hall");
        }
    }

    public TimeSlot getTimeSlotById(UUID id) {
        logger.info("Fetching time slot with ID: {}", id);
        try {
            Optional<TimeSlot> foundTimeSlot = timeSlotRepository.findById(id);
            return foundTimeSlot
                    .orElseThrow(() -> new BadRequestException("Time slot not found"));
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching time slot with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching time slot with ID: {}", id, e);
            throw new RuntimeException("Unexpected error fetching time slot");
        }
    }

    public List<Booking> getAllBookings() {
        logger.info("Fetching all bookings");
        try {
            return bookingRepository.findAll();
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching all bookings", ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (Exception e) {
            logger.error("Error fetching all bookings: {}", e.getMessage(), e);
            throw new RuntimeException("An error occurred while fetching all bookings.");
        }
    }

    public Booking getBookingById(UUID id) {
        logger.info("Fetching booking with bookingID: {}", id);
        try {
            Optional<Booking> booking = bookingRepository.findById(id);
            if (booking.isPresent()) {
                return booking.get();
            } else {
                logger.warn("No booking found for ID: {}", id);
                throw new BadRequestException("Booking id not found.");
            }
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching booking with ID: {}", id, ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (BadRequestException e) {
            // catch the exception thrown in the try block
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching booking with ID: {}", id, e);
            throw new RuntimeException("An error occurred while fetching the booking.");
        }
    }

    public List<Booking> getBookingsByUserId(UUID userId) {
        logger.info("Fetching bookings for userId: {}", userId);
        try {
            if (profileRepository.findById(userId).isEmpty()) {
                logger.error("User profile not found for userId: {}", userId);
                throw new BadRequestException("User profile not found.");
            }

            return bookingRepository.findByUserId(userId);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching bookings for userId: {}", userId, ex);
            throw new ServiceUnavailableException("The service is temporarily unavailable due to database issues. Please try again later.");
        } catch (BadRequestException e) {
            // catch the exception thrown in the try block
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching bookings for userId: {}", userId, e);
            throw new RuntimeException("An error occurred while fetching bookings for the user.");
        }
    }

    @Transactional(readOnly = true)
    public List<Booking> filterBookings(Optional<String> studentId,
                                        Optional<SportHallLocation> locationOpt,
                                        Optional<ProfileType> profileTypeOpt,
                                        Optional<Status> statusOpt) {
        // If no filters are applied, return all bookings
        if (studentId.isEmpty() && locationOpt.isEmpty() && profileTypeOpt.isEmpty() && statusOpt.isEmpty()) {
            return bookingRepository.findAll();
        }

        // Get all bookings
        List<Booking> bookings = bookingRepository.findAll();

        // Apply filters conditionally
        if (studentId.isPresent() && !studentId.get().isEmpty()) {
            String partialStudentId = studentId.get().toLowerCase();
            List<Profile> matchingProfiles = profileRepository.findAll().stream()
                    .filter(profile -> profile.getStudentId() != null &&
                                      profile.getStudentId().toLowerCase().contains(partialStudentId))
                    .collect(Collectors.toList());

            if (!matchingProfiles.isEmpty()) {
                List<UUID> matchingUserIds = matchingProfiles.stream()
                        .map(Profile::getId)
                        .collect(Collectors.toList());

                bookings = bookings.stream()
                        .filter(b -> matchingUserIds.contains(b.getUserId()))
                        .collect(Collectors.toList());
            }
        }

        if (statusOpt.isPresent()) {
            Status status = statusOpt.get();
            bookings = bookings.stream()
                    .filter(b -> b.getStatus().equals(status))
                    .collect(Collectors.toList());
        }

        if (locationOpt.isPresent()) {
            SportHallLocation location = locationOpt.get();
            bookings = bookings.stream()
                    .filter(b -> {
                        Optional<SportHall> hall = sportHallRepository.findById(b.getSportHallId());
                        return hall.isPresent() && hall.get().getLocation().equals(location);
                    })
                    .collect(Collectors.toList());
        }

        if (profileTypeOpt.isPresent()) {
            ProfileType profileType = profileTypeOpt.get();
            bookings = bookings.stream()
                    .filter(b -> {
                        Optional<Profile> profile = profileRepository.findById(b.getUserId());
                        return profile.isPresent() && profile.get().getType().equals(profileType);
                    })
                    .collect(Collectors.toList());
        }

        return bookings;
    }
}
