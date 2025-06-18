package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumInfoService {

    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private SportRepository sportRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private static final Logger logger = LoggerFactory.getLogger(StadiumInfoService.class);

    public ResponseEntity<?> getAllSportHalls() {
        logger.info("getAllSportHalls");
        try {
            List<?> sportHalls = sportHallRepository.findAll();
            if (sportHalls.isEmpty()) {
                logger.warn("No sport halls found");
                return ResponseEntity.status(404).body(List.of());
            }
            return ResponseEntity.ok(sportHalls);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sport halls", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching sport halls", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public ResponseEntity<?> getAllSports() {
        logger.info("Fetching all sports from the repository");
        try {
            List<?> sports = sportRepository.findAll();
            if (sports.isEmpty()) {
                logger.warn("No sports found");
                return ResponseEntity.status(404).body(List.of());
            }
            return ResponseEntity.ok(sports);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sports", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching sports", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public ResponseEntity<?> getAllTimeSlots() {
        logger.info("Fetching all time slots");
        try {
            List<?> timeSlots = timeSlotRepository.findAll();
            if (timeSlots.isEmpty()) {
                logger.warn("No time slots found");
                return ResponseEntity.status(404).body(List.of());
            }
            return ResponseEntity.ok(timeSlots);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching time slots", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching time slots", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
}
