package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.service.StadiumInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sports")
public class SportController {
    @Autowired
    private StadiumInfoService stadiumInfoService;

    @GetMapping("/get-sports")
    public ResponseEntity<?> getAllSports() {
        return stadiumInfoService.getAllSports();
    }
}


