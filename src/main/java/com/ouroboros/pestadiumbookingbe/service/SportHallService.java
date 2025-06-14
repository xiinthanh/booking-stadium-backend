package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SportHallService {

    @Autowired
    private SportHallRepository sportHallRepository;

    public List<SportHall> getAllSportHalls() {
        return sportHallRepository.findAll();
    }

    public SportHall getSportHallById(UUID id) {
        return sportHallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sport Hall not found with id: " + id));
    }
}
