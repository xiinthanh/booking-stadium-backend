package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SearchService {

    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private SportRepository sportRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SearchService.class);

    public ResponseEntity<?> getSportHallById(UUID id) {
        logger.info("Fetching sport hall with ID: {}", id);
        try {
            Optional<SportHall> foundSportHall = sportHallRepository.findById(id);
            if (foundSportHall.isPresent()) {
                return ResponseEntity.ok(foundSportHall.get());
            } else {
                logger.warn("No sport hall found for ID: {}", id);
                return ResponseEntity.status(404).body(null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sport hall with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching sport hall with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    public ResponseEntity<?> getTimeSlotById(UUID id) {
        logger.info("Fetching time slot with ID: {}", id);
        try {
            Optional<TimeSlot> foundTimeSlot = timeSlotRepository.findById(id);
            if (foundTimeSlot.isPresent()) {
                return ResponseEntity.ok(foundTimeSlot.get());
            } else {
                logger.warn("No time slot found for ID: {}", id);
                return ResponseEntity.status(404).body(null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching time slot with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching time slot with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
