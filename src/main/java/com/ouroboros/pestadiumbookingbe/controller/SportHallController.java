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
@RequestMapping("/api/sport-halls")
public class SportHallController {

    @Autowired
    private StadiumInfoService stadiumInfoService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/get-sport-halls")
    public ResponseEntity<?> getAllSportHalls() {
        return stadiumInfoService.getAllSportHalls();
    }

    @GetMapping("/get-sport-hall/{id}")
    public ResponseEntity<?> getSportHallById(@PathVariable UUID id) {
        return searchService.getSportHallById(id);
    }
}
