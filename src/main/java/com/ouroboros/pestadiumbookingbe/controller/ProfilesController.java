package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfilesController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/get-profiles")
    public ResponseEntity<List<Profile>> getAllProfiles() {
        List<Profile> profiles = profileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }

    @PreAuthorize("hasRole('ADMIN') or principal.userId == #id")
    @GetMapping("/get-profile/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable UUID id) {
        Profile profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profile);
    }

    @PreAuthorize("hasRole('ADMIN') or principal.userId == #profile.id")
    @PostMapping("/update-profile")
    public ResponseEntity<Profile> updateProfile(@RequestBody Profile profile) {
        Profile updated = profileService.updateProfile(profile);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN') or principal.userId == #id")
    @PostMapping("/delete-profile")
    public ResponseEntity<Void> deleteProfile(@RequestParam UUID id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/toggle-admin")
    public ResponseEntity<Profile> toggleAdmin(@RequestParam UUID id) {
        Profile updated = profileService.toggleAdmin(id);
        return ResponseEntity.ok(updated);
    }

}
