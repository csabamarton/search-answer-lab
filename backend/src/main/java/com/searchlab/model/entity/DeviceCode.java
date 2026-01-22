package com.searchlab.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for OAuth Device Code Flow.
 * Stores device codes that are pending user authorization.
 */
@Entity
@Table(name = "device_codes")
public class DeviceCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", nullable = false, unique = true, length = 100)
    private String deviceCode;

    @Column(name = "user_code", nullable = false, unique = true, length = 20)
    private String userCode;

    @Column(name = "verification_uri", nullable = false, length = 500)
    private String verificationUri;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds = 5; // Polling interval

    @Column(name = "user_id")
    private Long userId; // Set when user authorizes

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt; // Set when user authorizes

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public void setVerificationUri(String verificationUri) {
        this.verificationUri = verificationUri;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Check if device code is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if device code is authorized
     */
    public boolean isAuthorized() {
        return userId != null && authorizedAt != null;
    }
}
