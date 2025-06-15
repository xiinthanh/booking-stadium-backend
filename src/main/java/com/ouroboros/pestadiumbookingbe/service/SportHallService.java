package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SportHallService {

    @Autowired
    private SportHallRepository sportHallRepository;

    private static final Logger logger = LoggerFactory.getLogger(SportHallService.class);

    public List<SportHall> getAllSportHalls() {
        logger.info("getAllSportHalls");
        try {
            return sportHallRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching sport halls", e);
            return List.of();
        }
    }

    public SportHall getSportHallById(UUID id) {
        logger.info("Fetching sport hall with ID: {}", id);
        try {
            return sportHallRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Sport hall not found with ID: " + id));
        } catch (Exception e) {
            logger.error("Error fetching sport hall with ID: {}", id, e);
            return null;
        }
    }
}
