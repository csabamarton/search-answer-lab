# MCP Authentication Implementation Status

**Project:** Search Answer Lab - MCP Authentication Extension  
**Date Started:** January 20, 2025  
**Purpose:** Add secure authentication/authorization for MCP tool calls using OAuth Device Code Flow

---

## 🎯 Goal

Implement OAuth 2.0 Device Code Flow (RFC 8628) so that:
- Users must authenticate before Claude can search the knowledge base
- The LLM (Claude Desktop) never holds credentials
- The MCP server manages tokens (acts as the "wallet")
- The backend validates tokens and logs all access
- Users can revoke access anytime

---

## 📋 Implementation Phases

### Phase 1: Backend OAuth Infrastructure ✅ COMPLETE
**Status:** Steps 1-3 completed

#### Step 1: JWT Infrastructure ✅
- Added Spring Security OAuth dependencies
- Created `JwtService` for token generation and validation
- Configured JWT properties (secret, expiry times)
- Test endpoints: `/test/jwt/generate` and `/test/jwt/validate`

#### Step 2: Device Code Flow Endpoints ✅
- Created `DeviceCode` entity and repository
- Implemented three OAuth endpoints:
  - `POST /oauth/device/code` - Initiate device flow
  - `POST /oauth/device/authorize` - User authorization
  - `POST /oauth/device/token` - Polling endpoint
- Created `DeviceAuthService` with device code generation, authorization, and token polling
- Database migrations: V10 (device_codes table), V11 (admin password update)
- User authentication system with BCrypt password hashing

#### Step 3: Token Validation & Security ✅
- Created `JwtAuthenticationFilter` to extract and validate JWT tokens
- Created `JwtAuthenticationToken` for Spring Security context
- Updated `SecurityConfig` to protect `/api/search` endpoint
- Added `@PreAuthorize("hasAuthority('docs:search')")` to SearchController
- Created `SecurityExceptionHandler` for custom 401/403 error responses
- Protected endpoints require JWT token with `docs:search` scope

**Current State:**
- Backend can issue tokens via device flow
- Backend validates tokens on all protected endpoints
- `/api/search` requires authentication and `docs:search` scope
- OAuth endpoints remain public (no auth needed)

---

### Phase 2: MCP Server Integration ✅ COMPLETE
**Status:** Step 4 completed

#### Step 4: Create DeviceAuthManager (TypeScript) ✅
- TypeScript class to manage device flow
- Token storage (file-based for demo)
- Token refresh logic
- Expiry checking

**Detailed Implementation Plan:**

**Step 4.1: Create Type Definitions**
- File: `mcp-server/src/types/auth.ts`
- Types: `TokenResponse`, `DeviceCodeResponse`, `StoredTokens`, `AuthState`

**Step 4.2: Create TokenStorage Utility**
- File: `mcp-server/src/auth/TokenStorage.ts`
- Methods: `loadTokens()`, `saveTokens()`, `clearTokens()`, `getTokenPath()`
- Storage: `~/.search-answer-lab/tokens.json` (JSON format)
- Format: `{ accessToken, refreshToken, expiresAt, scopes, userId }`

**Step 4.3: Create DeviceAuthManager**
- File: `mcp-server/src/auth/DeviceAuthManager.ts`
- Main method: `ensureAuthenticated()` - Entry point for authentication
- Methods:
  - `initiateDeviceFlow()` - Call `POST /oauth/device/code`, show prompt
  - `pollForToken(deviceCode)` - Poll every 5 seconds until authorized
  - `refreshToken()` - Refresh expired tokens
  - `isTokenExpired()` - Check expiry with 5-minute buffer
  - `getAccessToken()` - Return current token, refresh if needed

**Step 4.4: Add Dependencies**
- `axios` or `node-fetch` - HTTP client
- `fs-extra` - File operations
- Update `package.json`

**Step 4.5: Update MCP Server Index**
- File: `mcp-server/src/index.ts`
- Initialize `DeviceAuthManager`
- Update `search_docs` tool to:
  - Call `ensureAuthenticated()` before request
  - Add `Authorization: Bearer <token>` header
  - Handle 401 errors (re-authenticate)
  - Show device code prompt via MCP protocol

**Key Features:**
- Automatic token refresh before expiry
- Device code flow with user prompts
- Token storage in `~/.search-answer-lab/tokens.json`
- Error handling for network failures and invalid tokens
- Integration with Claude Desktop UI for prompts

**Testing Scenarios:**
1. First-time authentication (no token)
2. Subsequent calls (token exists)
3. Token expiry (auto-refresh)
4. Invalid token (re-authentication)

#### Step 5: Update MCP Tools ✅
- ✅ Modified `search_docs` tool to use tokens
- ✅ Added `Authorization: Bearer <token>` header
- ✅ Handle authentication errors gracefully
- ✅ Non-blocking authentication with immediate error return
- ✅ Pending device code tracking and reuse
- ✅ Automatic authorization detection

---

### Phase 3: User Experience & Audit ✅ COMPLETE (Step 6)
**Status:** Step 6 implemented and tested successfully

#### Step 6: Build Authorization Page & Fix Tool Response ✅ COMPLETE
**Status:** Implemented and working  
**Completed:** January 21, 2025

**What Step 6 Implemented:**

1. **Fixed MCP Tool Response** ✅
   - Updated `searchDocs.ts` to return formatted auth instructions instead of throwing error
   - Transforms backend URL (port 8080) to frontend URL (port 3000)
   - Claude Desktop now shows full authentication instructions with clickable link
   - Message format: Clear step-by-step instructions with URL containing `?code=` parameter

2. **Built React Authorization Page** ✅
   - Created `AuthorizationPage.tsx` React component (replaced Thymeleaf approach)
   - Pre-fills device code from URL parameter (`?code=XXXX-XXXX`)
   - Username and password input fields
   - Real-time countdown timer (10 minutes)
   - Success/error message display
   - Styled with Tailwind CSS to match existing frontend design
   - Added route `/oauth/device/authorize` to React app

3. **Backend Updates** ✅
   - Updated `DeviceAuthController` to accept both JSON and form-urlencoded data
   - Fixed CORS configuration (removed conflicting `@CrossOrigin` annotation)
   - Added `/error` to public endpoints in SecurityConfig
   - Updated CORS config to include `/oauth/**` endpoints

4. **MCP Server Updates** ✅
   - Updated `server.ts` to handle content array responses directly
   - URL transformation from backend port to frontend port

**Result:**
- ✅ Claude Desktop shows clear auth instructions (no more "There was an error")
- ✅ User clicks link → React authorization page opens with pre-filled code
- ✅ User authorizes → Success message appears
- ✅ Subsequent searches work automatically with stored tokens
- ✅ Smooth, professional user experience

#### Step 7: Implement Audit Logging
- Audit events table
- Spring AOP aspect to log tool calls
- Record: userId, toolName, query, resultCount, timestamp

---

### Phase 4: Testing & Documentation 📋 PLANNED
**Status:** Not started

#### Step 8: End-to-End Testing
- Test device flow manually
- Test token expiry and refresh
- Test scope enforcement
- Test revocation
- Write documentation

---

## 📁 Key Files Created/Modified

### Backend (Java/Spring Boot)

**New Files:**
- `src/main/java/com/searchlab/service/JwtService.java` - JWT token generation/validation
- `src/main/java/com/searchlab/service/DeviceAuthService.java` - Device code flow logic
- `src/main/java/com/searchlab/service/UserService.java` - User authentication
- `src/main/java/com/searchlab/controller/DeviceAuthController.java` - OAuth endpoints
- `src/main/java/com/searchlab/controller/JwtTestController.java` - Test endpoints (temporary)
- `src/main/java/com/searchlab/controller/PasswordHashController.java` - Password hash utility
- `src/main/java/com/searchlab/security/JwtAuthenticationFilter.java` - JWT filter
- `src/main/java/com/searchlab/security/JwtAuthenticationToken.java` - Custom auth token
- `src/main/java/com/searchlab/security/SecurityExceptionHandler.java` - Error handlers
- `src/main/java/com/searchlab/model/entity/User.java` - User entity
- `src/main/java/com/searchlab/model/entity/DeviceCode.java` - Device code entity
- `src/main/java/com/searchlab/repository/UserRepository.java` - User repository
- `src/main/java/com/searchlab/repository/DeviceCodeRepository.java` - Device code repository
- `src/main/resources/db/migration/V6__create_users_table.sql` - Users table
- `src/main/resources/db/migration/V7__seed_default_user.sql` - Default admin user
- `src/main/resources/db/migration/V10__create_device_codes.sql` - Device codes table
- `src/main/resources/db/migration/V11__update_admin_password.sql` - Password update

**Modified Files:**
- `src/main/java/com/searchlab/config/SecurityConfig.java` - JWT security configuration
- `src/main/java/com/searchlab/controller/SearchController.java` - Added @PreAuthorize
- `src/main/java/com/searchlab/controller/DeviceAuthController.java` - Added refresh token endpoint
- `src/main/java/com/searchlab/service/DeviceAuthService.java` - Added refresh token method
- `pom.xml` - Added Spring Security OAuth and JWT dependencies
- `src/main/resources/application.yml` - JWT configuration, logging settings
- `postman-collections/Search-Answer-Lab-API.postman_collection.json` - Added OAuth and authenticated search endpoints

### MCP Server (TypeScript)

**New Files:**
- `src/types/auth.ts` - Type definitions for OAuth authentication
- `src/auth/TokenStorage.ts` - Token storage utility (file-based)
- `src/auth/DeviceAuthManager.ts` - Main authentication manager

**Modified Files:**
- `src/tools/searchDocs.ts` - Integrated authentication, returns auth instructions instead of errors
- `src/backendClient.ts` - Added Authorization header support
- `src/server.ts` - Updated to handle content array responses for auth instructions

### Frontend (React/TypeScript)

**New Files:**
- `src/pages/AuthorizationPage.tsx` - Device code authorization page component
- `src/services/authService.ts` - OAuth device code authorization API service

**Modified Files:**
- `src/App.tsx` - Added route for `/oauth/device/authorize`

---

## 🔑 Key Components

### OAuth Device Code Flow
1. **Initiate Flow:** `POST /oauth/device/code` → Returns `device_code`, `user_code`, `verification_uri`
2. **Authorize:** `POST /oauth/device/authorize` → User enters code + credentials
3. **Poll for Token:** `POST /oauth/device/token` → Returns `access_token` and `refresh_token`
4. **Refresh Token:** `POST /oauth/token` (grant_type=refresh_token) → Returns new access and refresh tokens

### MCP Server Authentication Features
- **Non-blocking mode:** Returns immediately with device code instructions instead of blocking
- **Pending device code tracking:** Reuses same device code across multiple requests
- **Automatic authorization detection:** Checks if device code was authorized before returning error
- **Token persistence:** Stores tokens in `~/.search-answer-lab/tokens.json`
- **Automatic token refresh:** Refreshes expired tokens before expiry (5-minute buffer)

### JWT Token Structure
- **Access Token:** 1 hour expiry, contains `userId` and `scopes` (`docs:search`, `docs:read`)
- **Refresh Token:** 30 days expiry, used to get new access tokens
- **Scopes:** `docs:search` (required for search), `docs:read` (future), `docs:admin` (future)

### Security Configuration
- **Public Endpoints:** `/api/health/**`, `/oauth/**`, `/test/**`, `/actuator/**`
- **Protected Endpoints:** `/api/search` (requires `docs:search` scope), `/api/admin/**`
- **Authentication:** JWT tokens validated via `JwtAuthenticationFilter`
- **Authorization:** Scope-based via `@PreAuthorize` annotations

---

## 🧪 Testing Status

### ✅ Completed Tests
- [x] JWT token generation works
- [x] JWT token validation works
- [x] Device code flow initiation works
- [x] Device code authorization works
- [x] Token polling works
- [x] Protected endpoint rejects unauthenticated requests (401)
- [x] Protected endpoint accepts authenticated requests (200)
- [x] Scope-based authorization works

### ✅ Completed Tests (Phase 2)
- [x] MCP server integration working
- [x] End-to-end flow from Claude Desktop
- [x] Device code flow with non-blocking mode
- [x] Token storage and retrieval
- [x] Pending device code tracking and reuse
- [x] Automatic authorization detection after user authenticates

### ✅ Completed Tests (Phase 3 - Step 6)
- [x] Authorization instructions visible in Claude Desktop
- [x] Frontend URL transformation (port 8080 → 3000)
- [x] React authorization page loads correctly
- [x] Device code pre-filled from URL parameter
- [x] Authorization form submission works
- [x] Success message displayed after authorization
- [x] Subsequent searches work without re-authentication
- [x] End-to-end flow tested successfully

### 📋 Remaining Tests
- [ ] Token expiry and auto-refresh (manual testing needed)
- [ ] Token revocation (no mechanism yet)

---

## 🔄 Current Status

**Phase 1: COMPLETE** ✅
- Backend OAuth infrastructure fully implemented
- JWT validation working
- Endpoints protected with scope-based authorization

**Phase 2: COMPLETE** ✅
- ✅ TypeScript `DeviceAuthManager` implemented in MCP server
- ✅ MCP tools updated to use authentication
- ✅ Complete flow from Claude Desktop tested and working
- ✅ Non-blocking authentication with pending device code reuse

**Phase 3: COMPLETE** ✅ (Step 6)
- ✅ Step 6: Authorization page & tool response fix (implemented with React frontend)
- Step 7: Audit logging (planned)

**Phase 4: PLANNED** 📋
- End-to-end testing and documentation

---

## 🚀 Next Steps

1. **Step 7:** Implement Audit Logging (Optional)
   - Audit events table
   - Track all tool calls with userId, toolName, query, resultCount, timestamp
   - Security monitoring

2. **Step 8:** End-to-End Testing & Documentation
   - Test token expiry and auto-refresh
   - Test token revocation
   - Write comprehensive documentation

3. **Future Enhancements:**
   - Token revocation endpoint
   - Multiple user support
   - Admin dashboard for viewing audit logs
   - Token refresh UI improvements

---

## 📝 Important Notes

### Default Test User
- **Username:** `admin`
- **Password:** `password`
- Created via migration V7, password updated in V11

### Token Storage
- Currently: File-based in MCP server (development)
- Production: Should use keychain/credential manager

### Environment Variables
- `JWT_SECRET` - Should be set in production (256+ bits)
- Default secret in `application.yml` is for development only

### Postman Collection
- All search endpoints updated to include `Authorization: Bearer {{accessToken}}`
- OAuth Device Code Flow endpoints with auto-save variables
- Test endpoints for JWT generation/validation

---

## 🔗 Related Documentation

- **Context Document:** `OPTION_4_MCP_AUTH_CONTEXT.md` (if exists)
- **Roadmap:** `OPTION_4_COMPLETE_ROADMAP.md` (if exists)
- **Step Guides:** Individual step implementation guides

---

## 📊 Architecture

```
Claude Desktop (LLM)
   ↓
MCP Server (TypeScript) - [✅ Phase 2: Authentication Complete]
   ├── DeviceAuthManager - Handles OAuth Device Code Flow
   ├── TokenStorage - Persists tokens to disk
   └── search_docs tool - Authenticated requests
   ↓
Spring Boot Backend :8080 - [✅ Phase 1: OAuth Infrastructure Complete]
   ├── OAuth Device Flow Endpoints (/oauth/device/*)
   ├── Token Refresh Endpoint (/oauth/token)
   ├── JWT Token Validation
   └── Protected Search Endpoint (/api/search)
   ↓
PostgreSQL :5433 - [📋 Step 7: Audit logging planned]
```

---

---

## 📘 Step 4: Detailed Implementation Plan

**Goal:** Implement OAuth Device Code Flow client in MCP server  
**Estimated Time:** 1-1.5 hours  
**Complexity:** Medium-Hard

### Components to Build

1. **DeviceAuthManager (Main Class)**
   - Manages device code flow lifecycle
   - Handles token storage and retrieval
   - Implements token refresh logic
   - Provides `ensureAuthenticated()` method

2. **TokenStorage (Utility)**
   - File-based token storage (JSON file)
   - Secure file handling
   - Token encryption (optional, for production)

3. **Type Definitions**
   - TypeScript interfaces for tokens, device codes, etc.

4. **Integration with MCP Tools**
   - Update `search_docs` tool to use authentication
   - Handle authentication errors gracefully

### Implementation Steps

**Step 4.1: Create Type Definitions**
- File: `mcp-server/src/types/auth.ts`
- Define: `TokenResponse`, `DeviceCodeResponse`, `StoredTokens`, `AuthState`

**Step 4.2: Create TokenStorage Utility**
- File: `mcp-server/src/auth/TokenStorage.ts`
- Methods: `loadTokens()`, `saveTokens()`, `clearTokens()`, `getTokenPath()`
- Storage location: `~/.search-answer-lab/tokens.json`
- File permissions: 600 (read/write owner only)

**Step 4.3: Create DeviceAuthManager**
- File: `mcp-server/src/auth/DeviceAuthManager.ts`
- Key methods:
  - `ensureAuthenticated()` - Main entry point, checks/refreshes/initiates auth
  - `initiateDeviceFlow()` - Calls `POST /oauth/device/code`, shows prompt
  - `pollForToken(deviceCode)` - Polls `POST /oauth/device/token` every 5 seconds
  - `refreshToken()` - Refreshes expired tokens
  - `isTokenExpired()` - Checks expiry with 5-minute buffer
  - `getAccessToken()` - Returns token, refreshes if needed

**Step 4.4: Add Dependencies**
- `axios` or `node-fetch` - HTTP client for API calls
- `fs-extra` - File system operations
- Update `package.json`

**Step 4.5: Update MCP Server Index**
- File: `mcp-server/src/index.ts`
- Initialize `DeviceAuthManager`
- Update `search_docs` tool:
  - Call `ensureAuthenticated()` before making request
  - Add `Authorization: Bearer <token>` header
  - Handle 401 errors (re-authenticate)
  - Show device code prompt via MCP protocol

### Device Code Flow Implementation

```
1. ensureAuthenticated() called
2. Check if token exists and is valid
   - If yes → return token
   - If expired → refresh token
   - If no → initiate device flow

3. Initiate device flow:
   - POST /oauth/device/code
   - Get device_code, user_code, verification_uri
   - Show prompt: "Visit {uri} and enter code: {user_code}"

4. Poll for token:
   - POST /oauth/device/token every 5 seconds
   - Wait for authorization (max 10 minutes)
   - Save tokens when received

5. Use token:
   - Add to Authorization header
   - Make authenticated request
```

### Token Storage Format

```typescript
{
  accessToken: string;
  refreshToken: string;
  expiresAt: number;  // Unix timestamp
  scopes: string[];
  userId: string;
}
```

### Error Handling Scenarios

1. **No token** → Initiate device flow
2. **Expired token** → Refresh or re-authenticate
3. **Invalid token** → Clear and re-authenticate
4. **401 Unauthorized** → Token invalid, re-authenticate
5. **403 Forbidden** → Missing scope, show error
6. **Network error** → Retry with exponential backoff

### Success Criteria

- [ ] `DeviceAuthManager` can initiate device flow
- [ ] Device code prompt shown in Claude Desktop
- [ ] Token polling works (waits for authorization)
- [ ] Tokens saved to file after authorization
- [ ] Tokens loaded from file on subsequent calls
- [ ] Token expiry detected and handled
- [ ] `search_docs` tool uses authentication
- [ ] 401 errors trigger re-authentication
- [ ] Clear error messages for auth failures

### Dependencies Required

```json
{
  "axios": "^1.6.0",
  "fs-extra": "^11.1.1",
  "@types/fs-extra": "^11.0.4"
}
```

### Environment Variables

```env
BACKEND_URL=http://localhost:8080
TOKEN_STORAGE_PATH=~/.search-answer-lab/tokens.json
```

---

**Last Updated:** January 21, 2025  
**Current Phase:** Phase 1, 2, and Step 6 Complete ✅  
**Next Action:** Step 7 - Implement Audit Logging (optional)

**Implementation Notes:**
- Core authentication architecture is complete and working
- Step 6 implemented: Authentication instructions now visible in Claude Desktop
- React authorization page provides smooth user experience
- End-to-end flow tested and working: Auth → Search → Results
- Frontend runs on port 3000, backend on port 8080
- MCP server transforms URLs from backend to frontend automatically
