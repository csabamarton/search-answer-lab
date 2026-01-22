package com.searchlab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchlab.model.entity.AuditEvent;
import com.searchlab.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for manual audit logging (for auth events, token operations, etc.)
 * Complements the AOP-based automatic logging for protected endpoints.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Log an authentication event (device flow initiation, authorization, etc.)
     */
    public void logAuthEvent(String eventType, String userId, String action, Map<String, Object> eventData, String status, String errorMessage) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setUserId(userId != null ? userId : "anonymous");
        auditEvent.setEventType(eventType);
        auditEvent.setToolName("auth");
        auditEvent.setAction(action);
        auditEvent.setStatus(status);
        auditEvent.setErrorMessage(errorMessage);
        auditEvent.setIpAddress(getIpAddress());
        
        if (eventData != null && !eventData.isEmpty()) {
            try {
                auditEvent.setEventData(objectMapper.writeValueAsString(eventData));
            } catch (Exception e) {
                logger.warn("Failed to serialize event data to JSON", e);
                auditEvent.setEventData(eventData.toString());
            }
        }
        
        auditEvent.setTimestamp(Instant.now());
        auditRepository.save(auditEvent);
        logger.debug("Audit logged: {} - {} - {}", eventType, action, status);
    }

    /**
     * Log token operation (refresh, revoke)
     */
    public void logTokenOperation(String operation, String userId, String status, String errorMessage) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("operation", operation);
        
        logAuthEvent("token_operation", userId, operation, eventData, status, errorMessage);
    }

    /**
     * Log device code flow initiation
     */
    public void logDeviceCodeRequest(String userCode, String deviceCode) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userCode", userCode);
        eventData.put("deviceCode", deviceCode);
        
        logAuthEvent("auth_device_code", "anonymous", "DEVICE_CODE_REQUESTED", eventData, "SUCCESS", null);
    }

    /**
     * Log device code authorization
     */
    public void logDeviceCodeAuthorization(String userCode, String userId, String username) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userCode", userCode);
        eventData.put("username", username);
        
        logAuthEvent("auth_authorize", userId, "DEVICE_CODE_AUTHORIZED", eventData, "SUCCESS", null);
    }

    /**
     * Log token issuance
     */
    public void logTokenIssuance(String userId, String deviceCode) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("deviceCode", deviceCode);
        
        logAuthEvent("token_issuance", userId, "TOKEN_ISSUED", eventData, "SUCCESS", null);
    }

    /**
     * Log authorization failure
     */
    public void logAuthFailure(String eventType, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("reason", reason);
        
        logAuthEvent(eventType, "anonymous", "AUTHORIZATION_FAILED", eventData, "FAILURE", reason);
    }

    /**
     * Get IP address (privacy-conscious - first 3 octets only)
     */
    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getRemoteAddr();
                // For privacy: show first 3 octets only (192.168.1.xxx)
                if (ip != null && ip.contains(".")) {
                    String[] parts = ip.split("\\.");
                    if (parts.length == 4) {
                        return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
                    }
                }
                return ip;
            }
        } catch (Exception e) {
            logger.debug("Could not get IP address", e);
        }
        return null;
    }
}
