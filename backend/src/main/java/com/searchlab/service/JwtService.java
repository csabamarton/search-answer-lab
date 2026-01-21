package com.searchlab.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token.expiry}")
    private long refreshTokenExpiry;

    /**
     * Generate access token with scopes
     */
    public String generateAccessToken(String userId, List<String> scopes) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .claim("scopes", scopes)
                .claim("token_type", "access")
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token (longer-lived, no scopes)
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate and parse JWT token
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extract scopes from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractScopes(String token) {
        Claims claims = validateToken(token);
        Object scopesObj = claims.get("scopes");
        if (scopesObj == null) {
            return List.of(); // Return empty list for refresh tokens or tokens without scopes
        }
        return (List<String>) scopesObj;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        return "refresh".equals(claims.get("token_type"));
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
