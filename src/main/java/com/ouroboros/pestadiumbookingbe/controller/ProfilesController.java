package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfilesController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/get-profiles")
    public List<Profile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @GetMapping("/get-profile/{id}")
    public Profile getProfileById(@PathVariable UUID id) {
        return profileService.getProfileById(id);
    }
    @PostMapping("/update-profile")
    public Profile updateProfile(Profile profile) {
        return profileService.updateProfile(profile);
    }
}
