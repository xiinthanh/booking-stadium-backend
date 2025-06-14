package com.ouroboros.pestadiumbookingbe.controller;


import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/sign-in")
    public ResponseEntity<?> googleSignIn(@RequestBody Map<String, String> requestBody) {
        String googleToken = requestBody.get("googleToken");

        // Validate the token with Supabase
        String supabaseUrl = System.getenv("NEXT_PUBLIC_SUPABASE_URL") + "/auth/v1/token?grant_type=google";
        String supabaseAnonKey = System.getenv("NEXT_PUBLIC_SUPABASE_ANON_KEY");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseAnonKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("access_token", googleToken);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(supabaseUrl, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
