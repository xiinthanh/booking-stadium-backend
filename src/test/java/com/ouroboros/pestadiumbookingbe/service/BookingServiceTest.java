package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import com.ouroboros.pestadiumbookingbe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private SportHallRepository sportHallRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;

    private UUID userId;
    private UUID hallId;
    private UUID sportId;
    private UUID timeSlotId;
    private LocalDate date;
    private String purpose;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        hallId = UUID.randomUUID();
        sportId = UUID.randomUUID();
        timeSlotId = UUID.randomUUID();
        date = LocalDate.now().plusDays(1);
        purpose = "Test Purpose";
    }

    @Test
    void testCreateBookingSuccess() {
        // Arrange
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        when(sportHallRepository.findById(hallId)).thenReturn(Optional.of(new SportHall()));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(new TimeSlot()));
        when(bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.confirmed)).thenReturn(0L);
        when(bookingRepository.countAndLockByUserIdAndBookingDateAndStatus(userId, date, Status.pending)).thenReturn(0L);
        when(bookingRepository.findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(hallId, date, timeSlotId, Status.pending))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(hallId, date, timeSlotId, Status.confirmed))
                .thenReturn(Collections.emptyList());
        Booking saved = new Booking();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setSportHallId(hallId);
        saved.setSportId(sportId);
        saved.setBookingDate(date);
        saved.setTimeSlotId(timeSlotId);
        saved.setPurpose(purpose);
        saved.setStatus(Status.pending);
        saved.setCreatedAt(OffsetDateTime.now());
        saved.setUpdatedAt(OffsetDateTime.now());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        // Act
        Booking result = bookingService.createBooking(userId, hallId, sportId, date, timeSlotId, purpose);

        // Assert
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void testConfirmBookingSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        Booking existing = new Booking();
        existing.setStatus(Status.pending);
        existing.setId(bookingId);
        when(bookingRepository.findAndLockById(bookingId)).thenReturn(Optional.of(existing));
        Booking confirmed = new Booking();
        confirmed.setId(bookingId);
        confirmed.setStatus(Status.confirmed);
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmed);

        // Act
        Booking result = bookingService.confirmBooking(bookingId, userId);

        // Assert
        assertEquals(Status.confirmed, result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void testCancelBookingSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        Booking existing = new Booking();
        existing.setStatus(Status.pending);
        existing.setId(bookingId);
        when(bookingRepository.findAndLockById(bookingId)).thenReturn(Optional.of(existing));
        Booking canceled = new Booking();
        canceled.setId(bookingId);
        canceled.setStatus(Status.rejected);
        canceled.setCanceledAt(OffsetDateTime.now());
        when(bookingRepository.save(any(Booking.class))).thenReturn(canceled);

        // Act
        Booking result = bookingService.cancelBooking(bookingId, userId);

        // Assert
        assertEquals(Status.rejected, result.getStatus());
        assertNotNull(result.getCanceledAt());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void testModifyBookingSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        when(sportHallRepository.findById(hallId)).thenReturn(Optional.of(new SportHall()));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(new TimeSlot()));
        Booking existing = new Booking();
        existing.setId(bookingId);
        existing.setStatus(Status.pending);
        existing.setBookingDate(date);
        existing.setSportHallId(hallId);
        existing.setTimeSlotId(timeSlotId);
        when(bookingRepository.findAndLockById(bookingId)).thenReturn(Optional.of(existing));
        Booking modified = new Booking();
        modified.setId(bookingId);
        modified.setStatus(Status.pending);
        when(bookingRepository.save(any(Booking.class))).thenReturn(modified);

        // Act
        Booking result = bookingService.modifyBooking(bookingId, userId, userId, hallId, sportId, date, timeSlotId, purpose);

        // Assert
        assertEquals(bookingId, result.getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void testDeleteBookingSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(new Profile()));
        Booking existing = new Booking();
        existing.setId(bookingId);
        when(bookingRepository.findAndLockById(bookingId)).thenReturn(Optional.of(existing));

        // Act
        bookingService.deleteBooking(bookingId, userId);

        // Assert
        verify(bookingRepository).delete(existing);
    }

    @Test
    void testGetAllBookings() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        var result = bookingService.getAllBookings();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBookingByIdSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        Booking existing = new Booking();
        existing.setId(bookingId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));

        // Act
        Booking result = bookingService.getBookingById(bookingId);

        // Assert
        assertEquals(bookingId, result.getId());
    }

    @Test
    void testGetBookingByIdNotFound() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> bookingService.getBookingById(bookingId));
    }
}
