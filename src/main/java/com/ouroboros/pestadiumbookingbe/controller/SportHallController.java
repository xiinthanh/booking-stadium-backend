package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.service.SportHallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sport-halls")
public class SportHallController {

    @Autowired
    private SportHallService sportHallService;

    @GetMapping("/get-sport-halls")
    public ResponseEntity<?> getAllSportHalls() {
        return sportHallService.getAllSportHalls();
    }

    @GetMapping("/get-sport-hall/{id}")
    public ResponseEntity<?> getSportHallById(@PathVariable UUID id) {
        return sportHallService.getSportHallById(id);
    }
}
