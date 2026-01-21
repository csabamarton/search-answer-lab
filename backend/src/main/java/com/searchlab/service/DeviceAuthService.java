package com.searchlab.service;

import com.searchlab.model.entity.DeviceCode;
import com.searchlab.model.entity.User;
import com.searchlab.repository.DeviceCodeRepository;
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
    private final JwtService jwtService;
    private final UserService userService;

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
            JwtService jwtService,
            UserService userService) {
        this.deviceCodeRepository = deviceCodeRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userService = userService;
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
            return new AuthorizationResult(false, "Invalid user code");
        }

        DeviceCode code = codeOpt.get();
        if (code.isExpired()) {
            logger.warn("Device code expired: user_code={}, expires_at={}", userCode, code.getExpiresAt());
            return new AuthorizationResult(false, "Device code expired");
        }

        if (code.isAuthorized()) {
            logger.warn("Device code already authorized: user_code={}, authorized_at={}", 
                    userCode, code.getAuthorizedAt());
            return new AuthorizationResult(false, "Device code already authorized");
        }

        // Authenticate user
        logger.debug("Authenticating user: username={}", username);
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isEmpty()) {
            logger.warn("Authentication failed: username={}", username);
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

        // Delete the device code (one-time use)
        deviceCodeRepository.delete(code);
        logger.info("Tokens generated and device code deleted: device_code={}, user_id={}, username={}", 
                deviceCode, user.getId(), user.getUsername());

        return Optional.of(new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600, // expires_in
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
