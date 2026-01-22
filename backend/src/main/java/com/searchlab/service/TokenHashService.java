package com.searchlab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service for hashing and verifying refresh tokens.
 * 
 * Refresh tokens are hashed using SHA-256 before storage in the database.
 * This provides security benefits:
 * - Tokens are not stored in plaintext
 * - Even if database is compromised, tokens cannot be extracted
 * - One-way hashing (cannot reverse hash to get original token)
 * 
 * Note: This is different from password hashing (which uses BCrypt).
 * Token hashing uses SHA-256 because:
 * - Tokens are already random/unguessable
 * - We need fast lookups (SHA-256 is faster than BCrypt)
 * - We're not protecting against rainbow tables (tokens are unique per user)
 */
@Service
public class TokenHashService {

    private static final Logger logger = LoggerFactory.getLogger(TokenHashService.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * Hash a refresh token using SHA-256.
     * Returns hex-encoded hash string (64 characters).
     * 
     * @param token The refresh token to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     * @throws RuntimeException if hashing fails (should never happen)
     */
    public String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            String hexHash = HEX_FORMAT.formatHex(hash);
            
            logger.debug("Token hashed successfully (length: {})", hexHash.length());
            return hexHash;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java implementations
            logger.error("SHA-256 algorithm not available - this should never happen", e);
            throw new RuntimeException("Failed to hash token: SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify that a token matches a stored hash.
     * 
     * @param token The token to verify
     * @param hash The stored hash to compare against
     * @return true if token matches hash, false otherwise
     */
    public boolean verifyToken(String token, String hash) {
        if (token == null || token.isEmpty() || hash == null || hash.isEmpty()) {
            return false;
        }

        String computedHash = hashToken(token);
        boolean matches = computedHash.equals(hash);
        
        logger.debug("Token verification: {}", matches ? "MATCH" : "NO MATCH");
        return matches;
    }
}
