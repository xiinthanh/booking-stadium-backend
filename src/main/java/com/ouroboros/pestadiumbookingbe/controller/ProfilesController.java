package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/get-profile/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable UUID id) {
        Profile profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profile);
    }
    @PostMapping("/update-profile")
    public ResponseEntity<Profile> updateProfile(@RequestBody Profile profile) {
        Profile updated = profileService.updateProfile(profile);
        return ResponseEntity.ok(updated);
    }
    @PostMapping("/delete-profile")
    public ResponseEntity<Void> deleteProfile(@RequestParam UUID id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/promote-admin")
    public ResponseEntity<Profile> promoteToAdmin(@RequestParam UUID id) {
        Profile updated = profileService.promoteToAdmin(id);
        return ResponseEntity.ok(updated);
    }
    @PostMapping("/demote-admin")
    public ResponseEntity<Profile> demoteFromAdmin(@RequestParam UUID id) {
        Profile updated = profileService.demoteFromAdmin(id);
        return ResponseEntity.ok(updated);
    }
}
