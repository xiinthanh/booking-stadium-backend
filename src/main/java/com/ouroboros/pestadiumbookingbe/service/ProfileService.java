package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;
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
            List<Profile> profiles = profileRepository.findAll();
            if (profiles.isEmpty()) {
                logger.warn("No profiles found");
                return ResponseEntity.status(404).body(List.of());
            }
            return ResponseEntity.ok(profiles);
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
            Optional<Profile> profile = profileRepository.findById(id);
            if (profile.isPresent()) {
                return ResponseEntity.ok(profile.get());
            } else {
                logger.warn("No profile found for ID: {}", id);
                return ResponseEntity.status(404).body(null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error fetching profile with ID: {}", id, ex);
            return ResponseEntity.status(503).body(null);
        } catch (Exception e) {
            logger.error("Error fetching profile with ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @Transactional(timeout = 2)
    public ResponseEntity<?> updateProfile(Profile profile) {
        logger.info("Updating profile with ID: {}", profile.getId());
        try {
            Optional<Profile> existingProfile = profileRepository.findAndLockById(profile.getId());

            if (existingProfile.isPresent()) {
                return ResponseEntity.ok(profileRepository.save(profile));
            } else {
                logger.warn("No profile found for ID: {}", profile.getId());
                return ResponseEntity.status(404).body(null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error updating profile with ID: {}", profile.getId(), ex);
            return ResponseEntity.status(503).body(null);
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while updating profile with ID: {}", profile.getId(), ex);
            return ResponseEntity.status(408).body(null);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid argument provided for profile update with ID: {}", profile.getId(), ex);
            return ResponseEntity.badRequest().body("Invalid profile data");
        } catch (Exception e) {
            logger.error("Error updating profile with ID: {}", profile.getId(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @Transactional(timeout = 2)
    public ResponseEntity<?> deleteProfile(UUID id) {
        logger.info("Deleting profile with ID: {}", id);
        try {
            Optional<Profile> profile = profileRepository.findAndLockById(id);
            if (profile.isPresent()) {
                profile.get().setDeleted(true);
                profileRepository.save(profile.get());
                logger.info("Profile with ID: {} deleted successfully", id);
                return ResponseEntity.ok("Profile deleted successfully");
            } else {
                logger.warn("No profile found for ID: {}", id);
                return ResponseEntity.status(404).body("Profile not found");
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Database error deleting profile with ID: {}", id, ex);
            return ResponseEntity.status(503).body("Service unavailable due to database issues");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while deleting profile with ID: {}", id, ex);
            return ResponseEntity.status(408).body("Request timeout while deleting profile");
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid argument provided for profile deletion with ID: {}", id, ex);
            return ResponseEntity.badRequest().body("Invalid profile ID");
        } catch (Exception e) {
            logger.error("Error deleting profile with ID: {}", id, e);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
