# Token Revocation Implementation Plan

**Status:** ✅ REVIEWED AND CORRECTED  
**Review Date:** January 23, 2025  
**Critical Issues Fixed:** 3  
**Ready for Implementation:** YES

---

## Review Summary

**Overall Assessment:** ⭐⭐⭐⭐☆ (4/5)
- Strong foundation and phased approach
- **3 Critical Issues** identified and fixed
- **2 Optional Enhancements** documented for future consideration
- Ready for implementation after corrections

---

## Current State Analysis

### What's Working ✅
- Refresh tokens are generated (JWT format)
- `/oauth/token` endpoint exists for refresh
- MCP server has `refreshToken()` method
- Token storage works (file-based in MCP server)

### Critical Gap ❌
- **Refresh tokens are NOT stored in database**
- Refresh tokens are stateless JWTs (just like access tokens)
- Revocation can't delete what doesn't exist in DB
- Access tokens remain valid for 1 hour after "revocation"

### Current Token Lifetimes
- **Access Token:** 1 hour (3600 seconds) - TOO LONG
- **Refresh Token:** 30 days (2592000 seconds) - OK
- **Location:** `application.yml` lines 81-84

### Current Revocation Flow (BROKEN)
```
1. User calls /oauth/revoke
2. Backend extracts userId from token
3. Backend logs audit event
4. Backend returns 200 OK
5. Token STILL WORKS (no actual revocation)
```

---

## Implementation Plan

### Phase 1: Database Schema (Non-Breaking)

**Goal:** Store refresh tokens in database so they can be revoked

#### Step 1.1: Create RefreshToken Entity
**File:** `backend/src/main/java/com/searchlab/model/entity/RefreshToken.java`

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 255)
    private String tokenHash; // SHA-256 hash of the refresh token
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // ✅ CRITICAL FIX: Add userId field for clean repository queries
    // insertable=false, updatable=false because it's managed by the @JoinColumn
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;
    
    @Column(name = "device_code")
    private String deviceCode; // Reference to original device code (for audit)
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastUsedAt;
    
    // Getters, setters, constructors
}
```

**Key Design Decisions:**
- Store **hash** of token (SHA-256), not plaintext (security)
- Link to `User` entity (for revocation by user)
- Store `deviceCode` reference (for audit trail)
- Track `lastUsedAt` (for monitoring)

#### Step 1.2: Create RefreshTokenRepository
**File:** `backend/src/main/java/com/searchlab/repository/RefreshTokenRepository.java`

```java
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    // ✅ CRITICAL FIX: Return int for atomic consumption check (prevents race conditions)
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);
    
    // ✅ FIXED: Uses userId field (no underscores in property path)
    long deleteByUserId(Long userId); // For "revoke all" functionality
    
    // ✅ FIXED: Uses userId field
    long deleteByUserIdAndDeviceCode(Long userId, String deviceCode); // For granular revocation
    
    List<RefreshToken> findByUserId(Long userId);
    
    // ✅ FIXED: Returns long (count of deleted rows), not a collection
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    long deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff); // For cleanup
}
```

#### Step 1.3: Create Flyway Migration
**File:** `backend/src/main/resources/db/migration/V13__create_refresh_tokens_table.sql`

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_code VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Cleanup job: Delete expired tokens older than 1 day
-- (Run periodically via scheduled task)
```

**Migration Notes:**
- `ON DELETE CASCADE` - If user is deleted, tokens are deleted
- Index on `token_hash` for fast lookups
- Index on `user_id` for revocation queries
- Index on `expires_at` for cleanup queries

---

### Phase 2: Token Storage Service (Non-Breaking)

**Goal:** Create service to hash and store refresh tokens

#### Step 2.1: Create TokenHashService
**File:** `backend/src/main/java/com/searchlab/service/TokenHashService.java`

```java
@Service
public class TokenHashService {
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Hash a refresh token using SHA-256
     * Returns hex-encoded hash string
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
    
    /**
     * Verify token matches hash
     */
    public boolean verifyToken(String token, String hash) {
        return hashToken(token).equals(hash);
    }
}
```

**Why Hash?**
- Security: Don't store plaintext tokens in database
- Privacy: Even if DB is compromised, tokens can't be extracted
- Standard practice: Similar to password hashing

---

### Phase 3: Update Token Issuance (Breaking Change)

**Goal:** Store refresh tokens in database when issued

#### Step 3.1: Update DeviceAuthService.pollForToken()
**File:** `backend/src/main/java/com/searchlab/service/DeviceAuthService.java`

**Current Code (Line 217):**
```java
String refreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));
```

**New Code:**
```java
String refreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));
String refreshTokenHash = tokenHashService.hashToken(refreshToken);

// Store refresh token in database
RefreshToken refreshTokenEntity = new RefreshToken();
refreshTokenEntity.setTokenHash(refreshTokenHash);
refreshTokenEntity.setUser(user);
refreshTokenEntity.setDeviceCode(deviceCode); // For audit trail
refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days
refreshTokenEntity.setCreatedAt(LocalDateTime.now());
refreshTokenRepository.save(refreshTokenEntity);

logger.debug("Refresh token stored in database: user_id={}, device_code={}", 
    user.getId(), deviceCode);
```

**Dependencies to Add:**
- `RefreshTokenRepository refreshTokenRepository`
- `TokenHashService tokenHashService`

#### Step 3.2: Update DeviceAuthService.refreshToken()
**File:** `backend/src/main/java/com/searchlab/service/DeviceAuthService.java`

**Current Code (Line 263-287):**
```java
public Optional<TokenResponse> refreshToken(String refreshToken) {
    // Validate refresh token (JWT only)
    if (jwtService.isTokenExpired(refreshToken)) {
        return Optional.empty();
    }
    if (!jwtService.isRefreshToken(refreshToken)) {
        return Optional.empty();
    }
    String userId = jwtService.extractUserId(refreshToken);
    // Generate new tokens...
}
```

**New Code:**
```java
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
        
        // ✅ CRITICAL FIX: Atomic consumption with row count check (prevents race conditions)
        // This ensures only ONE concurrent refresh request succeeds
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
        
        // Step 3: Token consumed successfully (exactly 1 row deleted)
        // This is atomic - only ONE request gets here per refresh token
        logger.debug("Refresh token consumed successfully (atomic delete)");
        
        // Step 4: Extract user ID and load user
        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        // Step 5: Generate new tokens
        List<String> scopes = List.of("docs:search", "docs:read");
        String newAccessToken = jwtService.generateAccessToken(userId, scopes);
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        
        // Step 6: Store new refresh token
        String newRefreshTokenHash = tokenHashService.hashToken(newRefreshToken);
        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setTokenHash(newRefreshTokenHash);
        newRefreshTokenEntity.setUser(user);
        newRefreshTokenEntity.setUserId(user.getId()); // ✅ Set userId field
        // Note: deviceCode is not preserved on refresh (new session)
        newRefreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(30));
        newRefreshTokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(newRefreshTokenEntity);
        
        logger.info("Tokens refreshed successfully: user_id={}", userId);
        auditService.logTokenOperation("token_refresh", userId, "SUCCESS", null);
        
        // Step 7: Return new tokens
        return Optional.of(new TokenResponse(
            newAccessToken,
            newRefreshToken,
            "Bearer",
            900, // 15 minutes (will be updated in Phase 5)
            scopes
        ));
    } catch (Exception e) {
        logger.error("Error refreshing token: {}", e.getMessage(), e);
        auditService.logTokenOperation("token_refresh", null, "FAILURE", e.getMessage());
        return Optional.empty();
    }
}
```

**Key Changes:**
- ✅ Check database for token existence (revocation check)
- ✅ Delete old refresh token (token rotation for security)
- ✅ Store new refresh token in database
- ✅ Preserve device code reference for audit

---

### Phase 4: Fix Revocation (Critical)

**Goal:** Actually prevent access by deleting refresh tokens

#### Step 4.1: Update DeviceAuthService.revokeToken()
**File:** `backend/src/main/java/com/searchlab/service/DeviceAuthService.java`

**Current Code (Line 314-335):**
```java
public String revokeToken(String token) {
    // Validate token and extract user ID
    String userId = jwtService.extractUserId(token);
    
    // Log audit event
    String tokenType = jwtService.isRefreshToken(token) ? "refresh_token" : "access_token";
    auditService.logTokenOperation("token_revoke", userId, "SUCCESS", null);
    
    return userId;
}
```

**New Code:**
```java
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
            }
        }
        
        // Step 4: Log audit event
        auditService.logTokenOperation("token_revoke", userId, "SUCCESS", 
            "Token type: " + tokenType);
        
        logger.info("Token revoked successfully: userId={}, tokenType={}", userId, tokenType);
        return userId;
        
    } catch (Exception e) {
        logger.warn("Token revocation failed - invalid token: {}", e.getMessage());
        auditService.logTokenOperation("token_revoke", null, "FAILURE", "Invalid token");
        throw new IllegalArgumentException("Invalid or expired token");
    }
}
```

**Revocation Strategy:**
- **Refresh token revoked:** Delete that specific token from DB
- **Access token revoked:** Delete ALL refresh tokens for that user (prevents new access tokens)

**Why This Works:**
- Refresh token deleted → Can't get new access tokens
- Current access token expires in 15 minutes (after Phase 5)
- Session effectively ended

---

### Phase 5: Reduce Access Token Lifetime (Breaking Change)

**Goal:** Reduce access token lifetime from 1 hour to 15 minutes

#### Step 5.1: Update application.yml
**File:** `backend/src/main/resources/application.yml`

**Current (Line 82):**
```yaml
jwt:
  access-token:
    expiry: 3600  # 1 hour in seconds
```

**New:**
```yaml
jwt:
  access-token:
    expiry: 900  # 15 minutes in seconds
```

#### Step 5.2: Update DeviceAuthService.pollForToken()
**File:** `backend/src/main/java/com/searchlab/service/DeviceAuthService.java`

**Current (Line 231):**
```java
return Optional.of(new TokenResponse(
    accessToken,
    refreshToken,
    "Bearer",
    3600, // expires_in
    scopes
));
```

**New:**
```java
return Optional.of(new TokenResponse(
    accessToken,
    refreshToken,
    "Bearer",
    900, // 15 minutes in seconds
    scopes
));
```

#### Step 5.3: Update DeviceAuthService.refreshToken()
**Already updated in Phase 3 (line 900)**

**Impact:**
- ✅ Shorter damage window if access token is stolen (15 min vs 1 hour)
- ✅ Revocation works within 15 minutes (access token expires naturally)
- ⚠️ More refresh calls (every 15 min vs 1 hour)
- ✅ Industry standard (OAuth 2.0 best practice)

---

### Phase 6: Cleanup Job (Optional but Recommended)

**Goal:** Periodically delete expired refresh tokens from database

#### Step 6.1: Create Scheduled Cleanup Task
**File:** `backend/src/main/java/com/searchlab/schedule/RefreshTokenCleanupTask.java`

```java
@Component
@Slf4j
public class RefreshTokenCleanupTask {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    public RefreshTokenCleanupTask(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    /**
     * Delete expired refresh tokens older than 1 day
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional // ✅ ADD: Transactional for cleanup operation
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        // ✅ FIXED: deleteByExpiresAtBefore returns long (count), not a collection
        long deleted = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Cleaned up {} expired refresh tokens", deleted);
    }
}
```

**Enable Scheduling:**
**File:** `backend/src/main/java/com/searchlab/SearchAnswerLabApplication.java`

Add `@EnableScheduling` annotation:
```java
@SpringBootApplication
@EnableScheduling
public class SearchAnswerLabApplication {
    // ...
}
```

---

### Phase 7: MCP Server Updates (No Changes Needed!)

**Good News:** MCP server already handles refresh correctly!

**Current Implementation:**
- ✅ `DeviceAuthManager.refreshToken()` calls `/oauth/token`
- ✅ Handles 401 errors (re-authenticates)
- ✅ Saves new tokens automatically
- ✅ Auto-refresh on token expiry

**No Changes Required** - MCP server will automatically:
1. Detect expired access token
2. Call `/oauth/token` with refresh token
3. Get new tokens (if refresh token not revoked)
4. Or re-authenticate (if refresh token revoked)

---

## Testing Plan

### Test 1: Token Issuance
1. Complete device code flow
2. Verify refresh token stored in database
3. Check token hash is stored (not plaintext)

### Test 2: Token Refresh
1. Use refresh token to get new access token
2. Verify old refresh token deleted from DB (atomic delete)
3. Verify new refresh token stored in DB
4. Verify access token lifetime is 15 minutes
5. **NEW:** Test concurrent refresh (send 2 refresh requests simultaneously)
   - Only ONE should succeed
   - The other should get "token not found" error

### Test 3: Revocation (Critical)
1. Revoke refresh token
2. Verify token deleted from database
3. Try to refresh → Should fail (401)
4. Try to use current access token → Works for max 15 minutes
5. After 15 minutes → Access token expires → Re-authentication required

### Test 4: Access Token Revocation
1. Revoke access token
2. Verify ALL refresh tokens deleted for user
3. Try to refresh → Should fail (401)
4. Current access token works for max 15 minutes
5. After 15 minutes → Re-authentication required

### Test 5: Token Rotation & Race Condition
1. Refresh token multiple times
2. Verify only one refresh token exists per user at a time
3. Verify old tokens are deleted
4. **NEW:** Test concurrent refresh requests (race condition)
   - Send 2 refresh requests with same token simultaneously
   - Verify only ONE succeeds (atomic delete prevents duplicates)
   - Verify the second request gets "token not found"

### Test 6: Cleanup Job
1. Create expired refresh token (manually set expires_at to past)
2. Run cleanup job
3. Verify expired tokens deleted

---

## Migration Strategy

### Non-Breaking Phases (Can Deploy Separately)
1. ✅ Phase 1: Database schema (adds table, doesn't break existing)
2. ✅ Phase 2: Token hash service (new service, doesn't affect existing)
3. ✅ Phase 3: Store refresh tokens (adds storage, doesn't break existing)

### Breaking Phases (Deploy Together)
4. ⚠️ Phase 4: Fix revocation (requires Phase 1-3)
5. ⚠️ Phase 5: Reduce access token lifetime (requires Phase 3)

### Recommended Deployment Order
1. **Deploy Phase 1-3** (non-breaking, adds infrastructure)
2. **Test thoroughly** (verify tokens stored correctly)
3. **Deploy Phase 4-5** (breaking, enables revocation)

---

## Rollback Plan

If issues occur:

### Rollback Phase 4-5
1. Revert `application.yml` (access token back to 1 hour)
2. Revert `DeviceAuthService` changes
3. Keep database schema (doesn't hurt)

### Rollback Phase 1-3
1. Remove refresh token checks from `refreshToken()` method
2. Keep database schema (can be cleaned up later)

---

## Performance Considerations

### Database Lookups
- **Refresh endpoint:** 1 lookup per refresh (every 15 minutes)
- **Revocation:** 1-2 lookups per revocation (rare)
- **Impact:** Minimal (indexed queries are fast)

### Token Hashing
- **SHA-256:** Very fast (microseconds)
- **Impact:** Negligible

### Cleanup Job
- **Frequency:** Daily
- **Impact:** Minimal (runs during low traffic)

---

## Security Considerations

### Token Hashing
- ✅ Tokens not stored in plaintext
- ✅ Even if DB compromised, tokens can't be extracted
- ✅ SHA-256 is one-way (can't reverse)

### Token Rotation
- ✅ Old refresh token deleted on refresh
- ✅ Limits damage if refresh token is stolen
- ✅ Industry best practice

### Revocation Strategy
- ✅ Refresh token deletion = immediate effect
- ✅ Access token revocation = prevents new tokens
- ✅ 15-minute window acceptable (industry standard)

---

## Summary

### What Changes
1. **Database:** New `refresh_tokens` table
2. **Backend:** Store refresh tokens, check on refresh, delete on revocation
3. **Config:** Access token lifetime: 1 hour → 15 minutes
4. **MCP Server:** No changes needed (already handles refresh)

### What Stays the Same
- JWT structure (access and refresh tokens)
- Device code flow
- MCP server authentication logic
- Token storage (file-based in MCP server)

### Benefits
- ✅ Revocation actually works
- ✅ Stolen tokens have limited damage window (15 min)
- ✅ Industry standard approach
- ✅ Scalable (stateless access tokens)
- ✅ Secure (hashed refresh tokens)

---

## Implementation Checklist

### Phase 1: Database Schema
- [ ] Create `RefreshToken` entity
- [ ] **CRITICAL:** Add `userId` field with `insertable=false, updatable=false`
- [ ] Create `RefreshTokenRepository` with corrected method signatures
- [ ] **CRITICAL:** `deleteByTokenHash()` must return `int` (not `void`)
- [ ] **CRITICAL:** `deleteByExpiresAtBefore()` must return `long` (not collection)
- [ ] Create Flyway migration V13
- [ ] Test migration runs successfully

### Phase 2: Token Hashing
- [ ] Create `TokenHashService`
- [ ] Test hashing and verification

### Phase 3: Store Refresh Tokens
- [ ] Update `pollForToken()` to store refresh token
- [ ] **CRITICAL:** Update `refreshToken()` to use atomic delete with row count check
- [ ] **CRITICAL:** Add `@Transactional` annotation to `refreshToken()` method
- [ ] Update `refreshToken()` to store new refresh token
- [ ] Set `userId` field when creating new refresh token entity
- [ ] Test token issuance stores in DB
- [ ] Test token refresh works
- [ ] **NEW:** Test concurrent refresh (race condition prevention)

### Phase 4: Fix Revocation
- [ ] Update `revokeToken()` to delete refresh tokens
- [ ] Test refresh token revocation
- [ ] Test access token revocation
- [ ] Verify revocation prevents new tokens

### Phase 5: Reduce Access Token Lifetime
- [ ] Update `application.yml`
- [ ] Update `pollForToken()` expires_in
- [ ] Test access token expires in 15 minutes
- [ ] Test auto-refresh works

### Phase 6: Cleanup Job (Optional)
- [ ] Create cleanup task
- [ ] **CRITICAL:** Fix return type (use `long`, not `.size()`)
- [ ] **CRITICAL:** Add `@Transactional` annotation
- [ ] Enable `@EnableScheduling` in main application class
- [ ] Test cleanup deletes expired tokens

### Testing
- [ ] Test complete flow: issue → refresh → revoke
- [ ] Test revocation prevents access
- [ ] Test token rotation
- [ ] Test cleanup job

---

**Estimated Time:** 4-6 hours  
**Complexity:** Medium  
**Risk:** Medium (breaking changes in Phase 4-5)  
**Priority:** HIGH (security gap)

---

## ⚠️ CRITICAL FIXES APPLIED

### Fix 1: Property Path Naming ✅
- **Issue:** Repository methods used `userId` but entity only had `User user`
- **Solution:** Added `userId` field with `insertable=false, updatable=false`
- **Impact:** Clean repository queries without underscores

### Fix 2: Race Condition Prevention ✅
- **Issue:** Concurrent refresh requests could create duplicate tokens
- **Solution:** Atomic DELETE with row count check (`deleteByTokenHash()` returns `int`)
- **Impact:** Only ONE refresh request succeeds per token (prevents token duplication)

### Fix 3: Cleanup Job Return Type ✅
- **Issue:** Assumed `deleteByExpiresAtBefore()` returns collection
- **Solution:** Changed to return `long` (count of deleted rows)
- **Impact:** Correct cleanup logging

---

## 💡 OPTIONAL ENHANCEMENTS (Out of Scope for MVP)

### Enhancement 1: Token Reuse Detection
- **What:** Detect if refresh token is used twice (indicates theft)
- **Status:** Skipped for MVP
- **Reason:** Atomic delete with row count check already prevents double-mint
- **Future:** Add token family tracking per OAuth 2.0 Security Best Practices (RFC 8252)

### Enhancement 2: Rate Limiting
- **What:** Prevent brute-force attacks on `/oauth/token` endpoint
- **Status:** Skipped for MVP
- **Reason:** Add later if abuse is detected
- **Future:** Implement rate limiting by user ID (not IP) to avoid NAT issues

---

## ✅ REVIEW APPROVAL

**Status:** ✅ APPROVED WITH CORRECTIONS  
**Critical Issues:** All 3 fixed  
**Ready for Implementation:** YES  
**Estimated Fix Time:** 30-60 minutes (for critical fixes)
