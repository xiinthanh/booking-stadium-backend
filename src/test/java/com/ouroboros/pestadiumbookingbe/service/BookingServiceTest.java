package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Test
    void testCreateBooking_databaseDown() {
        // Mock dependencies
        BookingRepository mockRepository = mock(BookingRepository.class);
        NotificationService mockNotificationService = mock(NotificationService.class);
        ProfileRepository mockProfileRepository = mock(ProfileRepository.class);

        // Simulate database downtime
        when(mockProfileRepository.findById(any(UUID.class))).thenThrow(new DataAccessException("Database is down") {});

        // Create BookingService instance with mocked dependencies
        BookingService bookingService = new BookingService();
        bookingService.bookingRepository = mockRepository;
        bookingService.notificationService = mockNotificationService;
        bookingService.profileRepository = mockProfileRepository;

        // Test data
        UUID userId = UUID.randomUUID();
        UUID sportHallId = UUID.randomUUID();
        UUID sportId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        UUID timeSlotId = UUID.randomUUID();
        String purpose = "Test booking";

        // Call the method
        ResponseEntity<?> response = bookingService.createBooking(userId, sportHallId, sportId, date, timeSlotId, purpose);

        // Verify response
        assertEquals(503, response.getStatusCode().value());
        assertEquals("The service is temporarily unavailable due to database issues. Please try again later.", response.getBody());

        // Verify repository interaction
        verify(mockProfileRepository, times(1)).findById(userId);
    }
}