package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.ConflictException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.exception.RequestTimeoutException;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionTimedOutException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceExceptionTest {

    @Mock BookingRepository bookingRepository;
    @Mock ProfileRepository profileRepository;
    @Mock SportHallRepository sportHallRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock NotificationService notificationService;

    @InjectMocks BookingService bookingService;

    private UUID userId;
    private UUID hallId;
    private UUID slotId;
    private UUID sportId;
    private LocalDate date;
    private String purpose;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        hallId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        sportId = UUID.randomUUID();
        date = LocalDate.now().plusDays(1);
        purpose = "test";
        // valid lookups
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        when(sportHallRepository.findById(hallId)).thenReturn(Optional.of(new SportHall()));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(new TimeSlot()));
    }

    @Test
    void createBooking_dataIntegrityViolation_throwsConflictException() {
        when(bookingRepository.save(any(Booking.class)))
            .thenThrow(new DataIntegrityViolationException("constraint"));
        assertThrows(ConflictException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, slotId, purpose)
        );
    }

    @Test
    void createBooking_dataAccessError_throwsServiceUnavailableException() {
        when(bookingRepository.save(any(Booking.class)))
            .thenThrow(new DataAccessResourceFailureException("db error"));
        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, slotId, purpose)
        );
    }

    @Test
    void createBooking_transactionTimedOut_throwsRequestTimeoutException() {
        when(bookingRepository.save(any(Booking.class)))
            .thenThrow(new TransactionTimedOutException("timeout"));
        assertThrows(RequestTimeoutException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, slotId, purpose)
        );
    }

    @Test
    void createBooking_unexpectedException_throwsRuntimeException() {
        when(bookingRepository.save(any(Booking.class)))
            .thenThrow(new RuntimeException("oops"));
        assertThrows(RuntimeException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, slotId, purpose)
        );
    }
}
