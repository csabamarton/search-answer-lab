package com.searchlab.service;

import com.searchlab.model.entity.DeviceCode;
import com.searchlab.model.entity.RefreshToken;
import com.searchlab.model.entity.User;
import com.searchlab.repository.DeviceCodeRepository;
import com.searchlab.repository.RefreshTokenRepository;
import com.searchlab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for OAuth Device Code Flow operations.
 */
@Service
public class DeviceAuthService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthService.class);

    private final DeviceCodeRepository deviceCodeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditService auditService;
    private final TokenHashService tokenHashService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    // Device code expiry: 10 minutes
    private static final int DEVICE_CODE_EXPIRY_MINUTES = 10;
    // Polling interval: 5 seconds
    private static final int POLLING_INTERVAL_SECONDS = 5;

    public DeviceAuthService(
            DeviceCodeRepository deviceCodeRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            UserService userService,
            AuditService auditService,
            TokenHashService tokenHashService) {
        this.deviceCodeRepository = deviceCodeRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userService = userService;
        this.auditService = auditService;
        this.tokenHashService = tokenHashService;
    }

    /**
     * Generate a new device code for device flow
     */
    @Transactional
    public DeviceCode generateDeviceCode() {
        logger.info("Generating new device code");
        
        // Generate unique codes
        String deviceCode = UUID.randomUUID().toString();
        String userCode = generateUserCode(); // Human-readable code like "WXYZ-5678"

        // Build verification URI (handle context path properly)
        String basePath = contextPath.equals("/") ? "" : contextPath;
        String verificationUri = String.format("http://localhost:%d%s/oauth/device/authorize",
                serverPort, basePath);

        // Create device code entity
        DeviceCode code = new DeviceCode();
        code.setDeviceCode(deviceCode);
        code.setUserCode(userCode);
        code.setVerificationUri(verificationUri);
        code.setExpiresAt(LocalDateTime.now().plusMinutes(DEVICE_CODE_EXPIRY_MINUTES));
        code.setIntervalSeconds(POLLING_INTERVAL_SECONDS);

        DeviceCode saved = deviceCodeRepository.save(code);
        logger.info("Device code generated: user_code={}, device_code={}, expires_at={}", 
                userCode, deviceCode, saved.getExpiresAt());
        
        // Log audit event
        auditService.logDeviceCodeRequest(userCode, deviceCode);
        
        return saved;
    }

    /**
     * Find device code by user code (for debugging)
     */
    public Optional<DeviceCode> findByUserCode(String userCode) {
        logger.debug("Looking up device code by user_code: {}", userCode);
        return deviceCodeRepository.findByUserCode(userCode);
    }

    /**
     * Authorize a device code with user credentials
     * @return AuthorizationResult with success status and error message if failed
     */
    @Transactional
    public AuthorizationResult authorizeDeviceCode(String userCode, String username, String password) {
        logger.debug("Attempting to authorize device code: user_code={}, username={}", userCode, username);
        
        Optional<DeviceCode> codeOpt = deviceCodeRepository.findByUserCode(userCode);
        if (codeOpt.isEmpty()) {
            logger.warn("Device code not found: user_code={}", userCode);
            auditService.logAuthFailure("auth_authorize", "Invalid user code");
            return new AuthorizationResult(false, "Invalid user code");
        }

        DeviceCode code = codeOpt.get();
        if (code.isExpired()) {
            logger.warn("Device code expired: user_code={}, expires_at={}", userCode, code.getExpiresAt());
            auditService.logAuthFailure("auth_authorize", "Device code expired");
            return new AuthorizationResult(false, "Device code expired");
        }

        if (code.isAuthorized()) {
            logger.warn("Device code already authorized: user_code={}, authorized_at={}", 
                    userCode, code.getAuthorizedAt());
            auditService.logAuthFailure("auth_authorize", "Device code already authorized");
            return new AuthorizationResult(false, "Device code already authorized");
        }

        // Authenticate user
        logger.debug("Authenticating user: username={}", username);
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isEmpty()) {
            logger.warn("Authentication failed: username={}", username);
            auditService.logAuthFailure("auth_authorize", "Invalid username or password");
            return new AuthorizationResult(false, "Invalid username or password");
        }

        User user = authenticatedUser.get();
        logger.info("User authenticated successfully: username={}, user_id={}", username, user.getId());

        // Authorize the device code
        code.setUserId(user.getId());
        code.setAuthorizedAt(LocalDateTime.now());
        deviceCodeRepository.save(code);

        logger.info("Device code authorized successfully: user_code={}, user_id={}, username={}", 
                userCode, user.getId(), username);
        
        // Log audit event
        auditService.logDeviceCodeAuthorization(userCode, String.valueOf(user.getId()), username);
        
        return new AuthorizationResult(true, null);
    }

    /**
     * Result of authorization attempt
     */
    public static class AuthorizationResult {
        private final boolean success;
        private final String errorMessage;

        public AuthorizationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Poll for token - check if device code is authorized and return tokens
     */
    @Transactional
    public Optional<TokenResponse> pollForToken(String deviceCode) {
        logger.debug("Polling for token: device_code={}", deviceCode);
        
        Optional<DeviceCode> codeOpt = deviceCodeRepository.findByDeviceCode(deviceCode);
        if (codeOpt.isEmpty()) {
            logger.warn("Device code not found during token poll: device_code={}", deviceCode);
            return Optional.empty();
        }

        DeviceCode code = codeOpt.get();

        // Check if expired
        if (code.isExpired()) {
            logger.warn("Device code expired during token poll: device_code={}, expires_at={}", 
                    deviceCode, code.getExpiresAt());
            return Optional.empty();
        }

        // Check if authorized
        if (!code.isAuthorized()) {
            logger.debug("Device code not yet authorized: device_code={}, user_code={}", 
                    deviceCode, code.getUserCode());
            // Return empty to indicate still pending
            return Optional.empty();
        }

        // Generate tokens
        logger.info("Device code authorized, generating tokens: device_code={}, user_id={}", 
                deviceCode, code.getUserId());
        
        User user = userRepository.findById(code.getUserId())
                .orElseThrow(() -> {
                    logger.error("User not found for authorized device code: device_code={}, user_id={}", 
                            deviceCode, code.getUserId());
                    return new IllegalStateException("User not found");
                });

        List<String> scopes = List.of("docs:search", "docs:read"); // Default scopes
        String accessToken = jwtService.generateAccessToken(String.valueOf(user.getId()), scopes);
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));

        // Store refresh token in database (hashed)
        String refreshTokenHash = tokenHashService.hashToken(refreshToken);
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .user(user)
                .userId(user.getId())
                .deviceCode(deviceCode)
                .expiresAt(LocalDateTime.now().plusDays(30)) // 30 days
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);
        logger.debug("Refresh token stored in database: user_id={}, device_code={}", 
                user.getId(), deviceCode);

        // Delete the device code (one-time use)
        deviceCodeRepository.delete(code);
        logger.info("Tokens generated and device code deleted: device_code={}, user_id={}, username={}", 
                deviceCode, user.getId(), user.getUsername());

        // Log audit event for token issuance
        auditService.logTokenIssuance(String.valueOf(user.getId()), deviceCode);

        return Optional.of(new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900, // 15 minutes in seconds (OAuth 2.0 best practice)
                scopes
        ));
    }

    /**
     * Generate a human-readable user code (e.g., "WXYZ-5678")
     */
    private String generateUserCode() {
        // Generate 8-character code, split with hyphen: "XXXX-YYYY"
        String part1 = generateRandomCode(4).toUpperCase();
        String part2 = generateRandomCode(4).toUpperCase();
        return part1 + "-" + part2;
    }

    /**
     * Generate random alphanumeric code
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing chars
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    /**
     * Refresh access token using refresh token.
     * 
     * CRITICAL: Uses atomic delete with row count check to prevent race conditions.
     * Only ONE concurrent refresh request can succeed per refresh token.
     * 
     * @return TokenResponse with new access and refresh tokens, or empty if refresh token is invalid/expired/revoked
     */
    @Transactional
    public Optional<TokenResponse> refreshToken(String refreshToken) {
        logger.debug("Refreshing token");
        
        try {
            // Step 1: Validate JWT structure and expiry
            if (jwtService.isTokenExpired(refreshToken)) {
                logger.warn("Refresh token expired (JWT)");
                auditService.logTokenOperation("token_refresh", null, "FAILURE", "Refresh token expired");
                return Optional.empty();
            }
            
            if (!jwtService.isRefreshToken(refreshToken)) {
                logger.warn("Token is not a refresh token");
                auditService.logTokenOperation("token_refresh", null, "FAILURE", "Token is not a refresh token");
                return Optional.empty();
            }
            
            // Step 2: Hash the refresh token
            String refreshTokenHash = tokenHashService.hashToken(refreshToken);
            
            // Step 3: CRITICAL FIX - Atomic consumption with row count check (prevents race conditions)
            // This ensures only ONE concurrent refresh request succeeds per token
            int deletedRows = refreshTokenRepository.deleteByTokenHash(refreshTokenHash);
            
            if (deletedRows == 0) {
                // Token not found (already used, revoked, or never existed)
                logger.warn("Refresh token not found in database (may be revoked or already used)");
                auditService.logTokenOperation("token_refresh", null, "FAILURE", 
                    "Refresh token not found or revoked");
                return Optional.empty();
            }
            
            if (deletedRows > 1) {
                // Should never happen (tokenHash is UNIQUE constraint)
                logger.error("Database anomaly: multiple tokens deleted for same hash");
                auditService.logTokenOperation("token_refresh", null, "FAILURE", 
                    "Database integrity issue");
                return Optional.empty();
            }
            
            // Step 4: Token consumed successfully (exactly 1 row deleted)
            // This is atomic - only ONE request gets here per refresh token
            logger.debug("Refresh token consumed successfully (atomic delete)");
            
            // Step 5: Extract user ID and load user
            String userId = jwtService.extractUserId(refreshToken);
            User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> {
                    logger.error("User not found: user_id={}", userId);
                    return new IllegalStateException("User not found: " + userId);
                });
            
            // Step 6: Generate new tokens
            List<String> scopes = List.of("docs:search", "docs:read");
            String newAccessToken = jwtService.generateAccessToken(userId, scopes);
            String newRefreshToken = jwtService.generateRefreshToken(userId);
            
            // Step 7: Store new refresh token in database
            String newRefreshTokenHash = tokenHashService.hashToken(newRefreshToken);
            RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                    .tokenHash(newRefreshTokenHash)
                    .user(user)
                    .userId(user.getId())
                    // Note: deviceCode is not preserved on refresh (new session)
                    .expiresAt(LocalDateTime.now().plusDays(30)) // 30 days
                    .createdAt(LocalDateTime.now())
                    .build();
            refreshTokenRepository.save(newRefreshTokenEntity);
            
            logger.info("Tokens refreshed successfully: user_id={}", userId);
            
            // Step 8: Log audit event
            auditService.logTokenOperation("token_refresh", userId, "SUCCESS", null);
            
            // Step 9: Return new tokens
            return Optional.of(new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    900, // 15 minutes in seconds (OAuth 2.0 best practice)
                    scopes
            ));
        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage(), e);
            auditService.logTokenOperation("token_refresh", null, "FAILURE", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Revoke a token (access or refresh).
     * 
     * Revocation strategy:
     * - If refresh token: Delete that specific token from database
     * - If access token: Delete ALL refresh tokens for that user (prevents new access tokens)
     * 
     * @param token The token to revoke (access or refresh token)
     * @return User ID from the revoked token
     * @throws Exception if token is invalid
     */
    @Transactional
    public String revokeToken(String token) {
        logger.debug("Revoking token");
        
        try {
            // Step 1: Validate token and extract user ID
            String userId = jwtService.extractUserId(token);
            String tokenType = jwtService.isRefreshToken(token) ? "refresh_token" : "access_token";
            
            // Step 2: If it's a refresh token, delete it from database
            if (jwtService.isRefreshToken(token)) {
                String refreshTokenHash = tokenHashService.hashToken(token);
                int deletedRows = refreshTokenRepository.deleteByTokenHash(refreshTokenHash);
                
                if (deletedRows > 0) {
                    logger.info("Refresh token revoked and deleted from database: user_id={}", userId);
                } else {
                    logger.warn("Refresh token not found in database (may already be revoked): user_id={}", userId);
                }
            } else {
                // Step 3: If it's an access token, revoke ALL refresh tokens for this user
                // (Access tokens expire quickly, but we want to prevent new tokens from being issued)
                long deletedCount = refreshTokenRepository.deleteByUserId(Long.parseLong(userId));
                if (deletedCount > 0) {
                    logger.info("All refresh tokens revoked for user: user_id={}, count={}", 
                        userId, deletedCount);
                } else {
                    logger.debug("No refresh tokens found for user: user_id={}", userId);
                }
            }
            
            // Step 4: Log audit event for token revocation
            auditService.logTokenOperation("token_revoke", userId, "SUCCESS", 
                "Token type: " + tokenType);
            
            logger.info("Token revoked successfully: userId={}, tokenType={}", userId, tokenType);
            return userId;
            
        } catch (Exception e) {
            logger.warn("Token revocation failed - invalid token: {}", e.getMessage());
            // Log audit event for revocation failure
            auditService.logTokenOperation("token_revoke", null, "FAILURE", "Invalid token");
            throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    /**
     * Response DTO for token polling
     */
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final int expiresIn;
        private final List<String> scopes;

        public TokenResponse(String accessToken, String refreshToken, String tokenType,
                           int expiresIn, List<String> scopes) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.scopes = scopes;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public List<String> getScopes() {
            return scopes;
        }
    }
}
