package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private static final Logger logger = LoggerFactory.getLogger(TimeSlotService.class);

    public ResponseEntity<?> getAllTimeSlots() {
        logger.info("Fetching all time slots");
        try {
            return ResponseEntity.ok(timeSlotRepository.findAll());
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching time slots", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching time slots", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public ResponseEntity<?> getTimeSlotById(UUID id) {
        logger.info("Fetching time slot with ID: {}", id);
        try {
            return ResponseEntity.ok(timeSlotRepository.findById(id)
                    .orElse(null));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching time slot with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching time slot with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
