# Token Revocation Testing Guide

## Overview

This guide helps you test the complete token revocation implementation (Phases 1-6).

**What Changed:**
- Refresh tokens are now stored in database (hashed)
- Token refresh checks database (revocation works!)
- Access tokens expire in 15 minutes (was 1 hour)
- Revocation deletes tokens from database
- Cleanup job runs on startup and daily

---

## Prerequisites

1. **Backend running** on `http://localhost:8080`
2. **Database running** (PostgreSQL)
3. **Postman** with collection imported
4. **Migration V13** should have run (check logs on startup)

---

## Test 1: Verify Refresh Token Storage

**Goal:** Confirm refresh tokens are stored in database when issued.

### Steps:

1. **Complete device code flow:**
   ```
   POST /oauth/device/code
   POST /oauth/device/authorize (user_code, username, password)
   POST /oauth/device/token (device_code)
   ```
   - Save the `refresh_token` from the response

2. **Check database:**
   ```sql
   SELECT id, token_hash, user_id, device_code, expires_at, created_at 
   FROM refresh_tokens 
   ORDER BY created_at DESC 
   LIMIT 5;
   ```
   - **Expected:** See a new row with your `user_id` (likely `1`)
   - **Expected:** `token_hash` is 64 characters (SHA-256 hex)
   - **Expected:** `expires_at` is ~30 days in the future

3. **Verify hash matches:**
   - The `token_hash` should be the SHA-256 hash of your refresh token
   - You can verify this using the `TokenHashService` (or manually with SHA-256)

**✅ Success Criteria:**
- Refresh token exists in database
- Hash is stored (not plaintext)
- User ID and device code are linked correctly

---

## Test 2: Token Refresh Works (With Database Check)

**Goal:** Verify refresh endpoint checks database and works correctly.

### Steps:

1. **Refresh the token:**
   ```
   POST /oauth/token
   Body: {
     "grant_type": "refresh_token",
     "refresh_token": "<your_refresh_token>"
   }
   ```
   - **Expected:** Returns new `access_token` and `refresh_token`
   - **Expected:** Old refresh token deleted from database
   - **Expected:** New refresh token stored in database

2. **Check database:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens WHERE user_id = 1;
   ```
   - **Expected:** Exactly 1 refresh token (old one deleted, new one stored)

3. **Verify token rotation:**
   - Try to use the OLD refresh token again
   - **Expected:** Should fail (token not found in database)

**✅ Success Criteria:**
- Refresh works and returns new tokens
- Old token deleted from database
- New token stored in database
- Old token can't be reused

---

## Test 3: Revocation Actually Works (CRITICAL)

**Goal:** Verify revocation prevents access.

### Steps:

1. **Get a fresh token:**
   - Complete device code flow again to get new tokens
   - Save the `access_token` and `refresh_token`

2. **Verify token works:**
   ```
   POST /api/search
   Authorization: Bearer <access_token>
   Body: {
     "query": "test",
     "mode": "semantic"
   }
   ```
   - **Expected:** Returns search results (200 OK)

3. **Revoke the refresh token:**
   ```
   POST /oauth/revoke
   Authorization: Bearer <refresh_token>
   ```
   - **Expected:** Returns 200 OK with message

4. **Check database:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens WHERE user_id = 1;
   ```
   - **Expected:** 0 refresh tokens (deleted)

5. **Try to refresh:**
   ```
   POST /oauth/token
   Body: {
     "grant_type": "refresh_token",
     "refresh_token": "<revoked_refresh_token>"
   }
   ```
   - **Expected:** Returns 401 Unauthorized
   - **Expected:** Error: "The refresh token is invalid or expired"
   - **Expected:** Token not found in database

6. **Current access token still works (temporarily):**
   ```
   POST /api/search
   Authorization: Bearer <access_token>
   ```
   - **Expected:** Still works (access token valid for max 15 minutes)
   - **Note:** After 15 minutes, it will expire and can't be refreshed

**✅ Success Criteria:**
- Revocation deletes token from database
- Refresh fails after revocation
- Access token works temporarily (until expiry)

---

## Test 4: Access Token Revocation

**Goal:** Verify revoking access token deletes ALL refresh tokens for user.

### Steps:

1. **Get tokens:**
   - Complete device code flow
   - Save `access_token`

2. **Revoke access token:**
   ```
   POST /oauth/revoke
   Authorization: Bearer <access_token>
   ```
   - **Expected:** Returns 200 OK

3. **Check database:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens WHERE user_id = 1;
   ```
   - **Expected:** 0 refresh tokens (all deleted)

4. **Try to refresh (any refresh token for this user):**
   ```
   POST /oauth/token
   Body: {
     "grant_type": "refresh_token",
     "refresh_token": "<any_refresh_token_for_user>"
   }
   ```
   - **Expected:** Fails (no refresh tokens in database)

**✅ Success Criteria:**
- All refresh tokens deleted for user
- No new access tokens can be issued
- Session effectively ended

---

## Test 5: Race Condition Prevention

**Goal:** Verify concurrent refresh requests don't create duplicate tokens.

### Steps:

1. **Get a refresh token:**
   - Complete device code flow
   - Save `refresh_token`

2. **Send 2 concurrent refresh requests:**
   - Use Postman's "Run Collection" with 2 iterations
   - OR use 2 Postman instances
   - Both use the SAME refresh token
   - Send both requests at the same time

3. **Check results:**
   - **Expected:** Only ONE request succeeds (200 OK)
   - **Expected:** The other request fails (401 Unauthorized)
   - **Expected:** Error: "The refresh token is invalid or expired"

4. **Check database:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens WHERE user_id = 1;
   ```
   - **Expected:** Exactly 1 refresh token (not 2)

**✅ Success Criteria:**
- Only one refresh succeeds
- No duplicate tokens created
- Atomic delete prevents race conditions

---

## Test 6: Access Token Lifetime (15 Minutes)

**Goal:** Verify access tokens expire in 15 minutes.

### Steps:

1. **Get a token:**
   - Complete device code flow
   - Note the `expires_in` value in response
   - **Expected:** `expires_in: 900` (15 minutes)

2. **Check JWT expiry:**
   - Decode the access token (JWT)
   - Check the `exp` claim
   - **Expected:** Expires 15 minutes from now

3. **Wait and test:**
   - Wait 16 minutes (or manually set system time)
   - Try to use the access token
   - **Expected:** Returns 401 Unauthorized (token expired)

**✅ Success Criteria:**
- `expires_in` is 900 seconds
- Token expires in 15 minutes
- Expired tokens are rejected

---

## Test 7: Cleanup Job

**Goal:** Verify cleanup job deletes expired tokens.

### Steps:

1. **Manually create expired token (for testing):**
   ```sql
   INSERT INTO refresh_tokens (token_hash, user_id, expires_at, created_at)
   VALUES (
     'test_hash_expired_' || EXTRACT(EPOCH FROM NOW())::text,
     1,
     NOW() - INTERVAL '2 days',  -- Expired 2 days ago
     NOW() - INTERVAL '2 days'
   );
   ```

2. **Check it exists:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens 
   WHERE expires_at < NOW() - INTERVAL '1 day';
   ```
   - **Expected:** At least 1 expired token

3. **Trigger cleanup (restart backend):**
   - Restart the backend application
   - **Expected:** See log: "Startup cleanup completed: deleted X expired refresh tokens"

4. **Check database:**
   ```sql
   SELECT COUNT(*) FROM refresh_tokens 
   WHERE expires_at < NOW() - INTERVAL '1 day';
   ```
   - **Expected:** 0 expired tokens (cleaned up)

**✅ Success Criteria:**
- Expired tokens deleted on startup
- Cleanup job runs successfully
- Logs show number of deleted tokens

---

## Test 8: End-to-End Flow

**Goal:** Test complete flow: issue → use → refresh → revoke → verify blocked.

### Steps:

1. **Issue tokens:**
   - Complete device code flow
   - Save tokens

2. **Use access token:**
   - Perform search with access token
   - **Expected:** Works

3. **Refresh token:**
   - Refresh to get new tokens
   - **Expected:** New tokens issued

4. **Revoke:**
   - Revoke refresh token
   - **Expected:** Token deleted from database

5. **Verify blocked:**
   - Try to refresh again
   - **Expected:** Fails (token not found)

6. **Wait for expiry:**
   - Wait 15+ minutes
   - Try to use access token
   - **Expected:** Fails (expired)

7. **Re-authenticate:**
   - Complete device code flow again
   - **Expected:** New tokens issued, works again

**✅ Success Criteria:**
- Complete flow works
- Revocation prevents access
- Re-authentication works

---

## Quick Test Checklist

Use this for a quick verification:

- [ ] **Refresh token stored in DB** (Test 1)
- [ ] **Token refresh works** (Test 2)
- [ ] **Revocation deletes token** (Test 3)
- [ ] **Revoked token can't refresh** (Test 3)
- [ ] **Access token revocation works** (Test 4)
- [ ] **Race condition prevented** (Test 5)
- [ ] **Access token expires in 15 min** (Test 6)
- [ ] **Cleanup job works** (Test 7)
- [ ] **End-to-end flow works** (Test 8)

---

## SQL Queries for Verification

### Check refresh tokens in database:
```sql
SELECT 
    id,
    LEFT(token_hash, 20) || '...' as token_hash_preview,
    user_id,
    device_code,
    expires_at,
    created_at,
    CASE 
        WHEN expires_at < NOW() THEN 'EXPIRED'
        ELSE 'VALID'
    END as status
FROM refresh_tokens
ORDER BY created_at DESC
LIMIT 10;
```

### Count tokens by user:
```sql
SELECT 
    user_id,
    COUNT(*) as token_count,
    COUNT(CASE WHEN expires_at < NOW() THEN 1 END) as expired_count
FROM refresh_tokens
GROUP BY user_id;
```

### Check for expired tokens:
```sql
SELECT COUNT(*) as expired_count
FROM refresh_tokens
WHERE expires_at < NOW() - INTERVAL '1 day';
```

---

## Troubleshooting

### Issue: Refresh token not found in database
- **Check:** Did migration V13 run? (check Flyway logs)
- **Check:** Is token being stored in `pollForToken()`?
- **Check:** Database connection working?

### Issue: Revocation doesn't work
- **Check:** Is `revokeToken()` deleting from database?
- **Check:** Are you revoking the correct token type?
- **Check:** Database transaction committed?

### Issue: Cleanup job not running
- **Check:** Is `@EnableScheduling` enabled?
- **Check:** Application logs for cleanup messages
- **Check:** Cron expression correct?

### Issue: Race condition still happens
- **Check:** Is `deleteByTokenHash()` returning `int`?
- **Check:** Is row count check in place?
- **Check:** `@Transactional` annotation present?

---

## Expected Database State

After running all tests, you should see:
- **refresh_tokens table:** Contains only valid (non-expired) tokens
- **No duplicate tokens:** Each user has at most 1 refresh token
- **Expired tokens cleaned:** Tokens expired > 1 day ago are deleted

---

**Ready to test!** 🧪
