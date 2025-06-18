package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.service.SportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sports")
public class SportController {
    @Autowired
    private SportService sportService;

    @GetMapping("/get-sports")
    public ResponseEntity<?> getAllSports() {
        return sportService.getAllSports();
    }
}


