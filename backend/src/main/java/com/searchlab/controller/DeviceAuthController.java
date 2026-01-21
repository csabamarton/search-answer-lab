package com.searchlab.controller;

import com.searchlab.model.entity.DeviceCode;
import com.searchlab.service.DeviceAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for OAuth 2.0 Device Code Flow (RFC 8628).
 * 
 * Endpoints:
 * - POST /oauth/device/code - Initiate device flow
 * - POST /oauth/device/authorize - User authorization
 * - POST /oauth/device/token - Poll for token
 * - POST /oauth/token - Refresh token (standard OAuth endpoint)
 */
@RestController
@RequestMapping("/oauth")
@CrossOrigin(origins = "*") // Allow CORS for device flow
public class DeviceAuthController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthController.class);
    private final DeviceAuthService deviceAuthService;

    public DeviceAuthController(DeviceAuthService deviceAuthService) {
        this.deviceAuthService = deviceAuthService;
    }

    /**
     * POST /oauth/device/code
     * Initiate device code flow - returns device_code and user_code
     */
    @PostMapping("/device/code")
    public ResponseEntity<Map<String, Object>> initiateDeviceFlow() {
        logger.info("Device flow initiation requested");
        DeviceCode deviceCode = deviceAuthService.generateDeviceCode();

        Map<String, Object> response = new HashMap<>();
        response.put("device_code", deviceCode.getDeviceCode());
        response.put("user_code", deviceCode.getUserCode());
        response.put("verification_uri", deviceCode.getVerificationUri());
        response.put("expires_in", 600); // 10 minutes in seconds
        response.put("interval", deviceCode.getIntervalSeconds());

        logger.info("Device flow initiated: user_code={}", deviceCode.getUserCode());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /oauth/device/authorize
     * User authorizes the device code by entering credentials
     */
    @PostMapping("/device/authorize")
    public ResponseEntity<Map<String, Object>> authorizeDeviceCode(
            @RequestBody Map<String, String> request) {
        
        String userCode = request.get("user_code");
        String username = request.get("username");
        String password = request.get("password");

        logger.info("Authorization request received: user_code={}, username={}", userCode, username);

        if (userCode == null || username == null || password == null) {
            logger.warn("Missing required parameters in authorization request: user_code={}, username={}", 
                    userCode != null, username != null);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_request");
            error.put("error_description", "Missing required parameters: user_code, username, password");
            return ResponseEntity.badRequest().body(error);
        }

        DeviceAuthService.AuthorizationResult result = 
                deviceAuthService.authorizeDeviceCode(userCode, username, password);

        if (result.isSuccess()) {
            logger.info("Authorization successful: user_code={}, username={}", userCode, username);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "authorized");
            response.put("message", "Device code authorized successfully");
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Authorization failed: user_code={}, username={}, reason={}", 
                    userCode, username, result.getErrorMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "authorization_failed");
            error.put("error_description", result.getErrorMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * POST /oauth/device/token
     * Poll for token - MCP server calls this repeatedly until user authorizes
     */
    @PostMapping("/device/token")
    public ResponseEntity<Map<String, Object>> pollForToken(
            @RequestBody Map<String, String> request) {
        
        String deviceCode = request.get("device_code");

        logger.debug("Token poll request: device_code={}", deviceCode);

        if (deviceCode == null) {
            logger.warn("Token poll request missing device_code");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_request");
            error.put("error_description", "Missing required parameter: device_code");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<DeviceAuthService.TokenResponse> tokenOpt = 
                deviceAuthService.pollForToken(deviceCode);

        if (tokenOpt.isEmpty()) {
            logger.debug("Token poll: authorization still pending for device_code={}", deviceCode);
            // Device code not found, expired, or not yet authorized
            Map<String, Object> error = new HashMap<>();
            error.put("error", "authorization_pending");
            error.put("error_description", "The authorization request is still pending");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        DeviceAuthService.TokenResponse tokenResponse = tokenOpt.get();
        logger.info("Token issued successfully: device_code={}", deviceCode);

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", tokenResponse.getAccessToken());
        response.put("refresh_token", tokenResponse.getRefreshToken());
        response.put("token_type", tokenResponse.getTokenType());
        response.put("expires_in", tokenResponse.getExpiresIn());
        response.put("scope", String.join(" ", tokenResponse.getScopes()));

        return ResponseEntity.ok(response);
    }

    /**
     * POST /oauth/token
     * Standard OAuth token endpoint for refreshing tokens
     * Supports grant_type=refresh_token
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody Map<String, String> request) {
        
        String grantType = request.get("grant_type");
        String refreshToken = request.get("refresh_token");

        logger.debug("Token refresh request: grant_type={}", grantType);

        if (!"refresh_token".equals(grantType)) {
            logger.warn("Invalid grant_type: {}", grantType);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "unsupported_grant_type");
            error.put("error_description", "Only grant_type=refresh_token is supported");
            return ResponseEntity.badRequest().body(error);
        }

        if (refreshToken == null) {
            logger.warn("Token refresh request missing refresh_token");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_request");
            error.put("error_description", "Missing required parameter: refresh_token");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<DeviceAuthService.TokenResponse> tokenOpt = 
                deviceAuthService.refreshToken(refreshToken);

        if (tokenOpt.isEmpty()) {
            logger.warn("Token refresh failed: invalid or expired refresh token");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_grant");
            error.put("error_description", "The refresh token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        DeviceAuthService.TokenResponse tokenResponse = tokenOpt.get();
        logger.info("Token refreshed successfully");

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", tokenResponse.getAccessToken());
        response.put("refresh_token", tokenResponse.getRefreshToken());
        response.put("token_type", tokenResponse.getTokenType());
        response.put("expires_in", tokenResponse.getExpiresIn());
        response.put("scope", String.join(" ", tokenResponse.getScopes()));

        return ResponseEntity.ok(response);
    }

    /**
     * GET /oauth/device/status?user_code=XXXX-YYYY
     * Debug endpoint to check device code status (dev only)
     */
    @GetMapping("/device/status")
    public ResponseEntity<Map<String, Object>> getDeviceCodeStatus(
            @RequestParam("user_code") String userCode) {
        
        logger.debug("Status check requested: user_code={}", userCode);
        Optional<DeviceCode> codeOpt = deviceAuthService.findByUserCode(userCode);
        
        if (codeOpt.isEmpty()) {
            logger.debug("Device code not found in status check: user_code={}", userCode);
            Map<String, Object> response = new HashMap<>();
            response.put("found", false);
            response.put("message", "Device code not found");
            return ResponseEntity.ok(response);
        }

        DeviceCode code = codeOpt.get();
        logger.debug("Device code status: user_code={}, expired={}, authorized={}", 
                userCode, code.isExpired(), code.isAuthorized());
        
        Map<String, Object> response = new HashMap<>();
        response.put("found", true);
        response.put("user_code", code.getUserCode());
        response.put("expired", code.isExpired());
        response.put("authorized", code.isAuthorized());
        response.put("expires_at", code.getExpiresAt());
        response.put("created_at", code.getCreatedAt());
        if (code.getAuthorizedAt() != null) {
            response.put("authorized_at", code.getAuthorizedAt());
        }
        if (code.getUserId() != null) {
            response.put("user_id", code.getUserId());
        }

        return ResponseEntity.ok(response);
    }
}
