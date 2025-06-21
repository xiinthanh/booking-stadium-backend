package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.service.SearchService;
import com.ouroboros.pestadiumbookingbe.service.StadiumInfoService;
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
    private StadiumInfoService stadiumInfoService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/get-sport-halls")
    public ResponseEntity<List<SportHall>> getAllSportHalls() {
        List<SportHall> halls = stadiumInfoService.getAllSportHalls();
        return ResponseEntity.ok(halls);
    }

    @GetMapping("/get-sport-hall/{id}")
    public ResponseEntity<SportHall> getSportHallById(@PathVariable UUID id) {
        SportHall hall = searchService.getSportHallById(id);
        return ResponseEntity.ok(hall);
    }
}
