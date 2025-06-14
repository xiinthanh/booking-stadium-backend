package com.ouroboros.pestadiumbookingbe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for Google Sign-In")
public class GoogleSignInRequest {

    @Schema(description = "Google OAuth token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
    private String googleToken;

    public String getGoogleToken() {
        return googleToken;
    }

    public void setGoogleToken(String googleToken) {
        this.googleToken = googleToken;
    }
}
