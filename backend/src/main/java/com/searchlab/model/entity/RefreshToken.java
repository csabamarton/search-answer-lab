package com.searchlab.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing refresh tokens in the database.
 * Refresh tokens are hashed (SHA-256) before storage for security.
 * 
 * This enables token revocation by deleting tokens from the database.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false, length = 255)
    private String tokenHash; // SHA-256 hash of the refresh token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // CRITICAL FIX: Add userId field for clean repository queries
    // insertable=false, updatable=false because it's managed by the @JoinColumn
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "device_code")
    private String deviceCode; // Reference to original device code (for audit trail)

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
