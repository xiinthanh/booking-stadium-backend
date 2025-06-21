package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.exception.RequestTimeoutException;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public SportHall getSportHallById(UUID id) {
        logger.info("Fetching sport hall with ID: {}", id);
        try {
            Optional<SportHall> foundSportHall = sportHallRepository.findById(id);
            return foundSportHall
                    .orElseThrow(() -> new BadRequestException("Sport hall not found"));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching sport hall with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (Exception e) {
            logger.error("Error fetching sport hall with ID: {}", id, e);
            throw new RuntimeException("Unexpected error fetching sport hall");
        }
    }

    public TimeSlot getTimeSlotById(UUID id) {
        logger.info("Fetching time slot with ID: {}", id);
        try {
            Optional<TimeSlot> foundTimeSlot = timeSlotRepository.findById(id);
            return foundTimeSlot
                    .orElseThrow(() -> new BadRequestException("Time slot not found"));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching time slot with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (Exception e) {
            logger.error("Error fetching time slot with ID: {}", id, e);
            throw new RuntimeException("Unexpected error fetching time slot");
        }
    }
}
