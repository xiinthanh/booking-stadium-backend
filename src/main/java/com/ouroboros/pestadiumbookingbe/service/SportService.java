package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SportService {

    private static final Logger logger = LoggerFactory.getLogger(SportService.class);

    @Autowired
    private SportRepository sportRepository;

    public List<Sport> getAllSports() {
        logger.info("Fetching all sports from the repository");
        return sportRepository.findAll();
    }
}

