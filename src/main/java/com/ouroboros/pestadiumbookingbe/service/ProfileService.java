package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileRepository profileRepository;

    public ResponseEntity<?> getAllProfiles() {
        logger.info("Fetching all profiles");
        try {
            return ResponseEntity.ok(profileRepository.findAll());
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching profiles", ex);
            return ResponseEntity.status(503).body(List.of());
        } catch (Exception e) {
            logger.error("Error fetching profiles", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public ResponseEntity<?> getProfileById(UUID id) {
        logger.info("Fetching profile with ID: {}", id);
        try {
            Profile foundProfile = profileRepository.findById(id)
                    .orElse(null);
            if (foundProfile == null) {
                logger.warn("No profile found for ID: {}", id);
                return ResponseEntity.status(404).body(null);
            }
            return ResponseEntity.ok(foundProfile);
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching profile with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching profile with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    public ResponseEntity<?> updateProfile(Profile profile) {
        logger.info("Updating profile with ID: {}", profile.getId());
        try {
            return ResponseEntity.ok(profileRepository.save(profile));
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error updating profile with ID: {}", profile.getId(), ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error updating profile with ID: {}", profile.getId(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    public ResponseEntity<String> getEmailByUserId(UUID userId) {
        logger.info("Fetching email for user ID: {}", userId);
        try {
            Optional<Profile> profile = profileRepository.findById(userId);
            if (profile.isPresent()) {
                return ResponseEntity.ok(profile.get().getEmail());
            } else {
                logger.warn("No profile found for user ID: {}", userId);
                return ResponseEntity.status(404).body(null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching email for user ID: {}", userId, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching email for user ID: {}", userId, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
