package com.searchlab.repository;

import com.searchlab.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for refresh tokens.
 * Provides query methods for token validation, revocation, and cleanup.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Delete refresh token by hash.
     * CRITICAL FIX: Returns int (row count) for atomic consumption check.
     * This prevents race conditions in concurrent refresh requests.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Delete all refresh tokens for a user (for "revoke all" functionality).
     * Uses userId field (no underscores in property path).
     */
    long deleteByUserId(Long userId);

    /**
     * Delete refresh token for specific user and device code (for granular revocation).
     * Uses userId field (no underscores in property path).
     */
    long deleteByUserIdAndDeviceCode(Long userId, String deviceCode);

    /**
     * Find all refresh tokens for a user.
     */
    List<RefreshToken> findByUserId(Long userId);

    /**
     * Delete expired refresh tokens (for cleanup job).
     * FIXED: Returns int (count of deleted rows) - Spring Data JPA modifying queries can only return void or int/Integer.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
