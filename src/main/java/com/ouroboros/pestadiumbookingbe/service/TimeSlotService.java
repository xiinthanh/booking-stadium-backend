package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private static final Logger logger = LoggerFactory.getLogger(TimeSlotService.class);

    public List<TimeSlot> getAllTimeSlots() {
        logger.info("Fetching all time slots");
        try {
            return timeSlotRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching time slots", e);
            return List.of();
        }
    }
}
