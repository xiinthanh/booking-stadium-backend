package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.repository.BookingRepository;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class BookingServiceDatabaseErrorTest {
    @MockitoBean
    private ProfileRepository profileRepository;
    @MockitoBean
    private SportHallRepository sportHallRepository;
    @MockitoBean
    private TimeSlotRepository timeSlotRepository;
    @MockitoBean
    private SportRepository sportRepository;
    @MockitoBean
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    private UUID userId;
    private UUID hallId;
    private UUID slotId;
    private UUID sportId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        hallId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        sportId = UUID.randomUUID();
    }

    @Test
    void createBooking_dataAccessException_throwsServiceUnavailable() {
        when(profileRepository.findById(userId))
                .thenThrow(new DataAccessResourceFailureException("Database error"));

        LocalDate date = LocalDate.now().plusDays(1);
        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose")
        );
    }
}
