package com.searchlab.schedule;

import com.searchlab.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task for cleaning up expired refresh tokens.
 * 
 * Runs:
 * - On application startup (cleans up expired tokens immediately)
 * - Daily at 2 AM (scheduled cleanup)
 * 
 * Deletes tokens that expired more than 1 day ago (buffer for safety).
 */
@Component
public class RefreshTokenCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupTask(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Cleanup expired refresh tokens on application startup.
     * Runs once when the application is fully started.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOnStartup() {
        logger.info("Running startup cleanup for expired refresh tokens...");
        long deleted = cleanupExpiredTokens();
        logger.info("Startup cleanup completed: deleted {} expired refresh tokens", deleted);
    }

    /**
     * Scheduled cleanup task - runs daily at 2 AM.
     * Deletes expired refresh tokens older than 1 day.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void scheduledCleanup() {
        logger.info("Running scheduled cleanup for expired refresh tokens...");
        long deleted = cleanupExpiredTokens();
        logger.info("Scheduled cleanup completed: deleted {} expired refresh tokens", deleted);
    }

    /**
     * Core cleanup logic - deletes expired refresh tokens.
     * Deletes tokens that expired more than 1 day ago (buffer for safety).
     * 
     * @return Number of tokens deleted
     */
    private long cleanupExpiredTokens() {
        try {
            // Delete tokens that expired more than 1 day ago
            // This buffer ensures we don't delete tokens that just expired
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            long deleted = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
            
            if (deleted > 0) {
                logger.info("Cleaned up {} expired refresh tokens (expired before {})", deleted, cutoff);
            } else {
                logger.debug("No expired refresh tokens to clean up");
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Error during refresh token cleanup: {}", e.getMessage(), e);
            return 0;
        }
    }
}
