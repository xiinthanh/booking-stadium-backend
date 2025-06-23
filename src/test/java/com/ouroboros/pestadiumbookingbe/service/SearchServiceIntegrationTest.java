package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import com.ouroboros.pestadiumbookingbe.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class SearchServiceIntegrationTest {

    @MockitoSpyBean
    private SearchService searchService;
    @MockitoSpyBean
    private SportHallRepository sportHallRepository;
    @MockitoSpyBean
    private SportRepository sportRepository;
    @MockitoSpyBean
    private TimeSlotRepository timeSlotRepository;

    private UUID hallId, slotId;

    @BeforeEach
    void setup() {
        Sport sport = new Sport();
        sport.setName("Test Sport");
        sport.setActive(true);
        sportRepository.save(sport);

        SportHall hall = new SportHall();
        hall.setSportId(sport.getId());
        hall.setName("Hall");
        hall.setLocation(com.ouroboros.pestadiumbookingbe.model.SportHallLocation.indoor);
        hall.setCapacity(5);
        sportHallRepository.save(hall);
        hallId = hall.getId();

        TimeSlot slot = new TimeSlot();
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(9, 0));
        slot.setDurationMinutes(60);
        slot.setActive(true);
        timeSlotRepository.save(slot);
        slotId = slot.getId();
    }

    @Test
    void getSportHallById_valid() {
        SportHall result = searchService.getSportHallById(hallId);
        assertNotNull(result);
        assertEquals(hallId, result.getId());
    }
    @Test
    void getSportHallById_invalid_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> searchService.getSportHallById(UUID.randomUUID()));
    }
    @Test
    void getSportHallById_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(sportHallRepository).findById(hallId);

        assertThrows(ServiceUnavailableException.class, () -> searchService.getSportHallById(hallId));
    }
    @Test
    void getSportHallById_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(sportHallRepository).findById(hallId);

        assertThrows(RuntimeException.class, () -> searchService.getSportHallById(hallId));
    }


    @Test
    void getTimeSlotById_valid() {
        TimeSlot result = searchService.getTimeSlotById(slotId);
        assertNotNull(result);
        assertEquals(slotId, result.getId());
    }
    @Test
    void getTimeSlotById_invalid_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> searchService.getTimeSlotById(UUID.randomUUID()));
    }
    @Test
    void getTimeSlotById_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(timeSlotRepository).findById(slotId);

        assertThrows(ServiceUnavailableException.class, () -> searchService.getTimeSlotById(slotId));
    }
    @Test
    void getTimeSlotById_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(timeSlotRepository).findById(slotId);

        assertThrows(RuntimeException.class, () -> searchService.getTimeSlotById(slotId));
    }
}
