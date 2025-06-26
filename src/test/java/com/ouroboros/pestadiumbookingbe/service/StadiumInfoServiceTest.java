package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class StadiumInfoServiceTest {

    @MockitoSpyBean
    private StadiumInfoService stadiumInfoService;
    @MockitoSpyBean
    private SportHallRepository sportHallRepository;
    @MockitoSpyBean
    private SportRepository sportRepository;

    @BeforeEach
    void clearDataAndInit() {
        sportHallRepository.deleteAll();
        sportRepository.deleteAll();

        Sport s = new Sport();
        s.setName("Sport");
        s.setActive(true);
        sportRepository.save(s);

        Sport s2 = new Sport();
        s2.setName("Another Sport");
        s2.setActive(true);
        sportRepository.save(s2);

        SportHall hall = new SportHall();
        hall.setSportId(s.getId());
        hall.setName("Hall");
        hall.setLocation(com.ouroboros.pestadiumbookingbe.model.SportHallLocation.indoor);
        hall.setCapacity(5);
        sportHallRepository.save(hall);

        SportHall hall2 = new SportHall();
        hall2.setSportId(s2.getId());
        hall2.setName("Another Hall");
        hall2.setLocation(com.ouroboros.pestadiumbookingbe.model.SportHallLocation.outdoor);
        hall2.setCapacity(10);
        sportHallRepository.save(hall2);
    }

    @Test
    void getAllSportHalls_empty_throwsServiceUnavailable() {
        sportHallRepository.deleteAll();
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSportHalls());
    }
    @Test
    void getAllSportHalls_returnsList() {
        List<SportHall> result = stadiumInfoService.getAllSportHalls();
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }
    @Test
    void getAllSportHalls_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(sportHallRepository).findAll();
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSportHalls());
    }
    @Test
    void getAllSportHalls_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(sportHallRepository).findAll();
        assertThrows(RuntimeException.class, () -> stadiumInfoService.getAllSportHalls());
    }

    @Test
    void getAllSports_empty_throwsServiceUnavailable() {
        sportRepository.deleteAll();
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSports());
    }
    @Test
    void getAllSports_returnsList() {
        List<Sport> sports = stadiumInfoService.getAllSports();
        assertFalse(sports.isEmpty());
        assertEquals(2, sports.size());
    }
    @Test
    void getAllSports_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(sportRepository).findAll();
        assertThrows(ServiceUnavailableException.class, () -> stadiumInfoService.getAllSports());
    }
    @Test
    void getAllSports_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(sportRepository).findAll();
        assertThrows(RuntimeException.class, () -> stadiumInfoService.getAllSports());
    }
}
