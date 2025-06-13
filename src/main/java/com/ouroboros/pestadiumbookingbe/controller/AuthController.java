package com.ouroboros.pestadiumbookingbe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ouroboros.pestadiumbookingbe.service.BookingService;
import com.ouroboros.pestadiumbookingbe.service.SupabaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class AuthController {

    @Autowired
    private SupabaseService supabaseService;

    @Operation(summary = "Sign in with Google", description = "Initiates Google OAuth2 login flow.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully signed in"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/signin/google")
    public ResponseEntity<?> signInWithGoogle(OAuth2AuthenticationToken authentication) {
        OAuth2User user = authentication.getPrincipal();
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        // Store user information in Supabase
        supabaseService.storeUser(email, name);

        // Store user information in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok("Signed in successfully as " + name + " (" + email + ")");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logOut() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }
}
