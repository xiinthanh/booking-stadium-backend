package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileRepository profileRepository;

    public List<Profile> getAllProfiles() {
        logger.info("Fetching all profiles");
        try {
            return profileRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching profiles", e);
            return List.of();
        }
    }

    public Profile getProfileById(UUID id) {
        logger.info("Fetching profile with ID: {}", id);
        try {
            return profileRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching profile with ID: {}", id, e);
            return null;
        }
    }

    public Profile updateProfile(Profile profile) {
        logger.info("Updating profile with ID: {}", profile.getId());
        try {
            return profileRepository.save(profile);
        } catch (Exception e) {
            logger.error("Error updating profile with ID: {}", profile.getId(), e);
            return null;
        }
    }
}
