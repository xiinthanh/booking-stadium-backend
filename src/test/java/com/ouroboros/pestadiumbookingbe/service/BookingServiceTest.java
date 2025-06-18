package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Status;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Test
    void testCreateBooking_databaseDown() {
        // 1. Mock dependencies
        BookingRepository mockRepository = mock(BookingRepository.class);
        NotificationService mockNotificationService = mock(NotificationService.class);

        // 2. Simulate database downtime
        when(mockRepository.existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                any(UUID.class), any(LocalDate.class), any(UUID.class), any()))
                .thenThrow(new DataAccessException("Database is down") {});

        // 3. Create service with mocks
        BookingService bookingService = new BookingService();
        bookingService.bookingRepository = mockRepository;
        bookingService.notificationService = mockNotificationService;

        // 4. Prepare test data
        UUID userId = UUID.randomUUID();
        UUID sportHallId = UUID.randomUUID();
        UUID sportId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        UUID timeSlotId = UUID.randomUUID();
        String purpose = "Test booking";

        // 5. Call the method
        ResponseEntity<?> response = bookingService.createBooking(userId, sportHallId, sportId, date, timeSlotId, purpose);

        // 6. Assert the response
        assertEquals(503, response.getStatusCode().value());
        assertEquals("The service is temporarily unavailable due to database issues. Please try again later.", response.getBody());

        // 7. Verify repository interaction
        verify(mockRepository, times(1)).existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(
                sportHallId, date, timeSlotId, Status.pending);
    }
}