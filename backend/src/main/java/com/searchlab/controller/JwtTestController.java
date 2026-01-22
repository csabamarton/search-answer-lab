package com.searchlab.controller;

import com.searchlab.service.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Temporary controller to test JWT generation
 * DELETE THIS after Step 1 testing is complete
 */
@RestController
@RequestMapping("/test/jwt")
public class JwtTestController {

    private final JwtService jwtService;

    public JwtTestController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/generate")
    public Map<String, Object> generateTestToken(@RequestBody Map<String, Object> request) {
        String userId = (String) request.getOrDefault("userId", "test-user");
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) request.getOrDefault(
            "scopes",
            List.of("docs:search", "docs:read")
        );

        String accessToken = jwtService.generateAccessToken(userId, scopes);
        String refreshToken = jwtService.generateRefreshToken(userId);

        return Map.of(
            "access_token", accessToken,
            "refresh_token", refreshToken,
            "token_type", "Bearer",
            "expires_in", 3600,
            "scopes", scopes
        );
    }

    @PostMapping("/validate")
    public Map<String, Object> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        try {
            String userId = jwtService.extractUserId(token);
            List<String> scopes = jwtService.extractScopes(token);
            boolean expired = jwtService.isTokenExpired(token);

            return Map.of(
                "valid", !expired,
                "userId", userId,
                "scopes", scopes,
                "expired", expired
            );
        } catch (Exception e) {
            return Map.of(
                "valid", false,
                "error", e.getMessage()
            );
        }
    }
}
