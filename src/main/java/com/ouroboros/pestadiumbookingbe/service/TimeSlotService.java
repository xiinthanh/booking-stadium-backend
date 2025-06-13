package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAll();
    }
}
