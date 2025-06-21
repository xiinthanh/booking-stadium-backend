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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class SearchServiceIntegrationTest {

    @Autowired
    private SearchService searchService;
    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private SportRepository sportRepository;
    @Autowired
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
    void getSportHallById_invalid_throws() {
        assertThrows(BadRequestException.class, () -> searchService.getSportHallById(UUID.randomUUID()));
    }

    @Test
    void getTimeSlotById_valid() {
        TimeSlot result = searchService.getTimeSlotById(slotId);
        assertNotNull(result);
        assertEquals(slotId, result.getId());
    }

    @Test
    void getTimeSlotById_invalid_throws() {
        assertThrows(BadRequestException.class, () -> searchService.getTimeSlotById(UUID.randomUUID()));
    }
}
