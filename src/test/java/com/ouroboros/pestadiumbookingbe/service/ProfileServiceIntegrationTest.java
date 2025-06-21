package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.RequestTimeoutException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ProfileServiceIntegrationTest {

    @Autowired
    private ProfileService profileService;
    @Autowired
    private ProfileRepository profileRepository;

    private Profile existing;

    @BeforeEach
    void init() {
        existing = new Profile();
        existing.setEmail("user@test.com");
        existing.setType(ProfileType.user);
        profileRepository.save(existing);
    }

    @Test
    void getAllProfiles_returnsList() {
        List<Profile> list = profileService.getAllProfiles();
        assertFalse(list.isEmpty());
    }

    @Test
    void getProfileById_validId() {
        Profile p = profileService.getProfileById(existing.getId());
        assertEquals(existing.getEmail(), p.getEmail());
    }

    @Test
    void getProfileById_invalidId_throws() {
        UUID id = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> profileService.getProfileById(id));
    }

    @Test
    void updateProfile_valid() {
        existing.setEmail("new@test.com");
        Profile updated = profileService.updateProfile(existing);
        assertEquals("new@test.com", updated.getEmail());
    }

    @Test
    void updateProfile_nonexistent_throws() {
        Profile fake = new Profile();
        fake.setId(UUID.randomUUID());
        fake.setEmail("x@test.com");
        fake.setType(ProfileType.admin);
        assertThrows(BadRequestException.class, () -> profileService.updateProfile(fake));
    }

    @Test
    void deleteProfile_setsDeleted() {
        profileService.deleteProfile(existing.getId());
        Profile p = profileRepository.findById(existing.getId()).orElseThrow();
        assertTrue(p.getDeleted());
    }

    @Test
    void deleteProfile_nonexistent_throws() {
        assertThrows(BadRequestException.class, () -> profileService.deleteProfile(UUID.randomUUID()));
    }
}
