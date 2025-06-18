package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SportService {

    private static final Logger logger = LoggerFactory.getLogger(SportService.class);

    @Autowired
    private SportRepository sportRepository;

    public ResponseEntity<?> getAllSports() {
        logger.info("Fetching all sports from the repository");
        try {
            return ResponseEntity.ok(sportRepository.findAll());
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sports", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching sports", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
}
