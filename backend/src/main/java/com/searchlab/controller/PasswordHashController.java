package com.searchlab.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility controller to generate BCrypt password hashes.
 * DELETE THIS after fixing the admin password.
 * 
 * Usage: POST /test/password/hash with body: {"password": "password"}
 */
@RestController
@RequestMapping("/test/password")
public class PasswordHashController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/hash")
    public Map<String, Object> generateHash(@RequestBody Map<String, String> request) {
        String password = request.getOrDefault("password", "password");
        String hash = passwordEncoder.encode(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("verified", passwordEncoder.matches(password, hash));
        
        return response;
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyHash(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");
        
        boolean matches = passwordEncoder.matches(password, hash);
        
        Map<String, Object> response = new HashMap<>();
        response.put("matches", matches);
        response.put("password", password);
        response.put("hash", hash);
        
        return response;
    }
}
