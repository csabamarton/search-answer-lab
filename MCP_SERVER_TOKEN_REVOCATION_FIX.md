# MCP Server Token Revocation Fix

## Problem

When tokens are revoked on the backend:
1. ✅ Backend deletes refresh tokens from database
2. ✅ Refresh fails (401/403) when trying to get new tokens
3. ❌ **MCP server still has tokens stored locally**
4. ❌ **Access token is still valid (not expired), so MCP server uses it**
5. ❌ **Search works even after revocation** (until access token expires)

## Root Cause

- **Access tokens are stateless JWTs** - validated by signature + expiry only
- **MCP server stores tokens locally** in `~/.search-answer-lab/tokens.json`
- **No check if tokens were revoked** before using stored access token
- **Access token remains valid** until expiry (max 15 minutes)

## Solution Implemented

### Changes Made:

1. **Clear tokens when refresh fails** (in `refreshToken()` method):
   - If refresh returns 401/403 or `invalid_grant`, clear local tokens
   - This happens when token is revoked or expired

2. **Clear tokens in `ensureAuthenticated()`**:
   - When refresh fails, clear local tokens before re-authenticating
   - Prevents using revoked tokens

3. **Clear tokens in `getAccessToken()`**:
   - When refresh fails, clear local tokens
   - Returns null to force re-authentication

### Code Changes:

```typescript
// In refreshToken() method:
if (response.status === 401 || response.status === 403 || errorData.error === "invalid_grant") {
  console.error("[DeviceAuthManager] Refresh failed - token likely revoked, clearing local tokens");
  await clearTokens();
  this.pendingDeviceCode = null;
  // ...
}

// In ensureAuthenticated() method:
catch (error) {
  console.error(`[DeviceAuthManager] Token refresh failed: ${error}`);
  // Clear local tokens when refresh fails
  await clearTokens();
  this.pendingDeviceCode = null;
  // Fall through to re-authentication
}
```

## How It Works Now

### Scenario 1: Access Token Still Valid (Not Expired)

```
Time 0:00 - Issue access token (expires at 0:15)
Time 0:05 - Revoke access token (backend deletes refresh tokens)
Time 0:10 - MCP server uses stored access token → WORKS (still valid JWT)
Time 0:15 - Access token expires
Time 0:16 - MCP server tries to refresh → FAILS (refresh tokens deleted)
         → Local tokens cleared ✅
         → Must re-authenticate ✅
```

**Note:** Access token still works until expiry (expected behavior for stateless JWTs).

### Scenario 2: Access Token Expired

```
Time 0:00 - Issue access token (expires at 0:15)
Time 0:05 - Revoke access token (backend deletes refresh tokens)
Time 0:16 - Access token expires
Time 0:17 - MCP server tries to refresh → FAILS (refresh tokens deleted)
         → Local tokens cleared ✅
         → Must re-authenticate ✅
```

**Result:** Revocation is enforced when token expires.

## Limitations

### Access Token Still Valid Until Expiry

- **Access tokens are stateless JWTs** - can't be immediately invalidated
- **Revocation prevents future access** (by deleting refresh tokens)
- **Current access token works** until expiry (max 15 minutes)

This is **expected behavior** and follows OAuth 2.0 best practices:
- Short-lived access tokens (15 min) limit damage window
- Revocation prevents new tokens
- No database overhead on every request

### For Immediate Revocation

If you need **immediate** access token invalidation, you would need:
1. **Token blacklist** - Store revoked tokens in database
2. **Check blacklist** - In `JwtAuthenticationFilter` before accepting token
3. **Performance trade-off** - Database lookup on every request

## Testing

### Test Revocation with MCP Server:

1. **Authenticate via Claude Desktop:**
   - Search works, tokens stored in MCP server

2. **Revoke token via Postman:**
   ```
   POST /oauth/revoke
   Authorization: Bearer <access_token>
   ```

3. **Wait for access token to expire** (or manually expire it)

4. **Try search again:**
   - MCP server tries to refresh → FAILS
   - Local tokens cleared ✅
   - Must re-authenticate ✅

5. **Verify tokens cleared:**
   - Check `~/.search-answer-lab/tokens.json` → Should not exist

## Summary

✅ **Fixed:** MCP server now clears local tokens when refresh fails (revoked/expired)

✅ **Result:** Revocation is enforced when access token expires

⚠️ **Limitation:** Access token still works until expiry (expected for stateless JWTs)

**This is the best we can do without implementing a token blacklist.**
