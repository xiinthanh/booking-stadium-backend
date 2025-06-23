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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ProfileServiceIntegrationTest {

    @MockitoSpyBean
    private ProfileService profileService;
    @MockitoSpyBean
    private ProfileRepository profileRepository;

    private Profile existing;
    private Profile anotherExisting;

    @BeforeEach
    void init() {
        existing = new Profile();
        existing.setEmail("user@test.com");
        existing.setType(ProfileType.user);
        profileRepository.save(existing);

        anotherExisting = new Profile();
        anotherExisting.setEmail("anotherUser@test.com");
        anotherExisting.setType(ProfileType.user);
        profileRepository.save(anotherExisting);
    }

    @Test
    void getAllProfiles_returnsList() {
        List<Profile> list = profileService.getAllProfiles();
        assertEquals(2, list.size());
    }
    @Test
    void getAllProfiles_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(profileRepository).findAll();
        assertThrows(ServiceUnavailableException.class, () -> profileService.getAllProfiles());
    }
    @Test
    void getAllProfiles_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(profileRepository).findAll();
        assertThrows(RuntimeException.class, () -> profileService.getAllProfiles());
    }


    @Test
    void getProfileById_validId() {
        Profile p = profileService.getProfileById(existing.getId());
        assertEquals(existing.getEmail(), p.getEmail());
    }
    @Test
    void getProfileById_dataAccessResourceFailure_throwsServiceUnavailable() {
        UUID id = existing.getId();
        doThrow(DataAccessResourceFailureException.class)
                .when(profileRepository).findById(id);
        assertThrows(ServiceUnavailableException.class, () -> profileService.getProfileById(id));
    }
    @Test
    void getProfileById_genericException_throwsRuntimeException() {
        UUID id = existing.getId();
        doThrow(RuntimeException.class)
                .when(profileRepository).findById(id);
        assertThrows(RuntimeException.class, () -> profileService.getProfileById(id));
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
    void updateProfile_invalidUser_throwsBadRequest() {
        Profile fake = new Profile();
        fake.setId(UUID.randomUUID());
        fake.setEmail("x@test.com");
        fake.setType(ProfileType.admin);
        assertThrows(BadRequestException.class, () -> profileService.updateProfile(fake));
    }
    @Test
    void updateProfile_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(profileRepository).findAndLockById(existing.getId());
        assertThrows(ServiceUnavailableException.class, () -> profileService.updateProfile(existing));
    }
    @Test
    void updateProfile_transactionTimeout_throwsRequestTimeout() {
        doThrow(TransactionTimedOutException.class)
                .when(profileRepository).findAndLockById(existing.getId());
        assertThrows(RequestTimeoutException.class, () -> profileService.updateProfile(existing));
    }

    @Test
    void updateProfile_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(profileRepository).findAndLockById(existing.getId());
        assertThrows(RuntimeException.class, () -> profileService.updateProfile(existing));
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
