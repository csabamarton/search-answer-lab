package com.searchlab.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit event entity for tracking all security-related operations.
 * Records who did what, when, and the outcome.
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who performed the action (from JWT token or device flow)
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of event: search, auth_device_code, auth_authorize, token_refresh, token_revoke, etc.
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * Tool or action name: search_docs, device_code_requested, etc.
     */
    @Column(name = "tool_name", length = 100)
    private String toolName;

    /**
     * Action performed: READ, WRITE, AUTHENTICATE, AUTHORIZE, etc.
     */
    @Column(name = "action", length = 50)
    private String action;

    /**
     * Event details as JSON (query, resultCount, mode, etc.)
     */
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    /**
     * Status: SUCCESS, FAILURE
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Error message if status is FAILURE
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Duration in milliseconds (for performance tracking)
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Request ID for tracing (from traceId header or generated)
     */
    @Column(name = "request_id", length = 100)
    private String requestId;

    /**
     * IP address (first 3 octets only for privacy: 192.168.1.xxx)
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Timestamp when event occurred
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
