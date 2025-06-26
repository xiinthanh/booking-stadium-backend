package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumInfoService {

    @Autowired
    private SportHallRepository sportHallRepository;
    @Autowired
    private SportRepository sportRepository;

    private static final Logger logger = LoggerFactory.getLogger(StadiumInfoService.class);

    public List<SportHall> getAllSportHalls() {
        logger.info("getAllSportHalls");
        try {
            List<SportHall> sportHalls = sportHallRepository.findAll();
            if (sportHalls.isEmpty()) {
                logger.warn("No sport halls found");
                throw new ServiceUnavailableException("No sport halls found");
            }
            return sportHalls;
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching sport halls", ex);
            throw new ServiceUnavailableException("Database error fetching sport halls", ex);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching sport halls", e);
            throw new ServiceUnavailableException("Error fetching sport halls", e);
        }
    }

    public List<Sport> getAllSports() {
        logger.info("Fetching all sports from the repository");
        try {
            List<Sport> sports = sportRepository.findAll();
            if (sports.isEmpty()) {
                logger.warn("No sports found");
                throw new ServiceUnavailableException("No sports found");
            }
            return sports;
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching sports", ex);
            throw new ServiceUnavailableException("Database error fetching sports", ex);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching sports", e);
            throw new ServiceUnavailableException("Error fetching sports", e);
        }
    }
}
