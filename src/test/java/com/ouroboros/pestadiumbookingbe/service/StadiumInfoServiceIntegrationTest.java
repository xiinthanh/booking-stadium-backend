package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class StadiumInfoServiceIntegrationTest {

    @Autowired
    private StadiumInfoService stadiumInfoService;
    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private SportRepository sportRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @BeforeEach
    void clearData() {
        sportHallRepository.deleteAll();
        sportRepository.deleteAll();
        timeSlotRepository.deleteAll();
    }

    @Test
    void getAllSportHalls_empty_throws() {
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSportHalls());
    }

    @Test
    void getAllSportHalls_returnsList() {
        Sport s = new Sport();
        s.setName("Sport");
        s.setActive(true);
        sportRepository.save(s);
        SportHall hall = new SportHall();
        hall.setSportId(s.getId());
        hall.setName("Hall");
        hall.setLocation(com.ouroboros.pestadiumbookingbe.model.SportHallLocation.indoor);
        hall.setCapacity(5);
        sportHallRepository.save(hall);

        List<SportHall> result = stadiumInfoService.getAllSportHalls();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getAllSports_empty_throws() {
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSports());
    }

    @Test
    void getAllSports_returnsList() {
        Sport s = new Sport();
        s.setName("Sport");
        s.setActive(true);
        sportRepository.save(s);

        List<Sport> sports = stadiumInfoService.getAllSports();
        assertFalse(sports.isEmpty());
        assertEquals(1, sports.size());
    }

    @Test
    void getAllTimeSlots_empty_throws() {
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllTimeSlots());
    }

    @Test
    void getAllTimeSlots_returnsList() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setDurationMinutes(60);
        slot.setActive(true);
        timeSlotRepository.save(slot);

        List<TimeSlot> slots = stadiumInfoService.getAllTimeSlots();
        assertFalse(slots.isEmpty());
        assertEquals(1, slots.size());
    }
}
