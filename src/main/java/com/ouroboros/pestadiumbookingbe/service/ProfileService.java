package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.RequestTimeoutException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
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
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching profiles", ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (Exception e) {
            logger.error("Error fetching profiles", e);
            throw new RuntimeException("Unexpected error fetching profiles");
        }
    }

    public Profile getProfileById(UUID id) {
        logger.info("Fetching profile with ID: {}", id);
        try {
            return profileRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error fetching profile with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching profile with ID: {}", id, e);
            throw new RuntimeException("Unexpected error fetching profile");
        }
    }

    @Transactional(timeout = 2)
    public Profile updateProfile(Profile profile) {
        logger.info("Updating profile with ID: {}", profile.getId());
        try {
            profileRepository.findAndLockById(profile.getId())
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            return profileRepository.save(profile);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error updating profile with ID: {}", profile.getId(), ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while updating profile with ID: {}", profile.getId(), ex);
            throw new RequestTimeoutException("Request timed out while updating profile");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating profile with ID: {}", profile.getId(), e);
            throw new RuntimeException("Unexpected error updating profile");
        }
    }

    @Transactional(timeout = 2)
    public void deleteProfile(UUID id) {
        logger.info("Deleting profile with ID: {}", id);
        try {
            Profile existing = profileRepository.findAndLockById(id)
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            existing.setDeleted(true);
            profileRepository.save(existing);
            logger.info("Profile with ID: {} deleted successfully", id);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error deleting profile with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while deleting profile with ID: {}", id, ex);
            throw new RequestTimeoutException("Request timed out while deleting profile");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting profile with ID: {}", id, e);
            throw new RuntimeException("Unexpected error deleting profile");
        }
    }

    @Transactional(timeout = 2)
    public Profile promoteToAdmin(UUID id) {
        logger.info("Promoting profile with ID: {} to admin", id);
        try {
            Profile profile = profileRepository.findAndLockById(id)
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            if (profile.getType() == ProfileType.admin) {
                logger.warn("Profile with ID: {} is already an admin", id);
                return profile; // Already an admin, no change needed
            }
            profile.setType(ProfileType.admin);
            return profileRepository.save(profile);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error promoting profile with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while promoting profile with ID: {}", id, ex);
            throw new RequestTimeoutException("Request timed out while promoting profile");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error promoting profile with ID: {}", id, e);
            throw new RuntimeException("Unexpected error promoting profile");
        }
    }

    @Transactional(timeout = 2)
    public Profile demoteFromAdmin(UUID id) {
        logger.info("Demoting profile with ID: {} from admin", id);
        try {
            Profile profile = profileRepository.findAndLockById(id)
                    .orElseThrow(() -> new BadRequestException("Profile not found"));
            if (profile.getType() != ProfileType.admin) {
                logger.warn("Profile with ID: {} is not an admin", id);
                return profile; // Not an admin, no change needed
            }
            profile.setType(ProfileType.user);
            return profileRepository.save(profile);
        } catch (DataAccessResourceFailureException ex) {
            logger.error("Database error demoting profile with ID: {}", id, ex);
            throw new ServiceUnavailableException("Service unavailable due to database issues");
        } catch (TransactionTimedOutException ex) {
            logger.error("Transaction timed out while demoting profile with ID: {}", id, ex);
            throw new RequestTimeoutException("Request timed out while demoting profile");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error demoting profile with ID: {}", id, e);
            throw new RuntimeException("Unexpected error demoting profile");
        }
    }
}
