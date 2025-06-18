package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.service.SearchService;
import com.ouroboros.pestadiumbookingbe.service.StadiumInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private StadiumInfoService stadiumInfoService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/get-time-slots")
    public ResponseEntity<?> getAllTimeSlots() {
        return stadiumInfoService.getAllTimeSlots();
    }

    @GetMapping("/get-time-slot/{id}")
    public ResponseEntity<?> getTimeSlot(@PathVariable UUID id) {
        return searchService.getTimeSlotById(id);
    }
}
