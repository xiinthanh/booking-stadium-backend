package com.ouroboros.pestadiumbookingbe.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SupabaseService supabaseService;

    public CustomOAuth2UserService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract user information
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Store user information in Supabase
        supabaseService.storeUser(email, name);

        return oAuth2User;
    }
}
