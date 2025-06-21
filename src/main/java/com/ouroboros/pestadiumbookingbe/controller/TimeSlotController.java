package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.service.SearchService;
import com.ouroboros.pestadiumbookingbe.service.StadiumInfoService;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private StadiumInfoService stadiumInfoService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/get-time-slots")
    public ResponseEntity<List<TimeSlot>> getAllTimeSlots() {
        List<TimeSlot> slots = stadiumInfoService.getAllTimeSlots();
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/get-time-slot/{id}")
    public ResponseEntity<TimeSlot> getTimeSlot(@PathVariable UUID id) {
        TimeSlot slot = searchService.getTimeSlotById(id);
        return ResponseEntity.ok(slot);
    }
}
