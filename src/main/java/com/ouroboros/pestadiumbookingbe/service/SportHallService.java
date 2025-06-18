package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SportHallService {

    @Autowired
    private SportHallRepository sportHallRepository;

    private static final Logger logger = LoggerFactory.getLogger(SportHallService.class);

    public ResponseEntity<?> getAllSportHalls() {
        logger.info("getAllSportHalls");
        try {
            return ResponseEntity.ok(sportHallRepository.findAll());
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sport halls", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching sport halls", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public ResponseEntity<?> getSportHallById(UUID id) {
        logger.info("Fetching sport hall with ID: {}", id);
        try {
            return ResponseEntity.ok(sportHallRepository.findById(id)
                    .orElse(null));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sport hall with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching sport hall with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
