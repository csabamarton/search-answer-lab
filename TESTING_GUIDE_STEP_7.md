# Testing Guide for Step 7: Audit Logging & Token Revocation

## Prerequisites

1. **Backend is running** on `http://localhost:8080`
2. **Postman** is installed and the collection is imported
3. **Database** is running (PostgreSQL)
4. **Audit table** exists (check with `V12__create_audit_events_table.sql` migration)

---

## Test 1: Audit Logging - Device Code Flow

**Goal:** Verify that authentication events are logged to the audit table.

### Steps:

1. **Check initial audit count:**
   ```
   GET /api/admin/audit/stats
   Authorization: Bearer <token>
   ```
   Note the initial counts.

2. **Initiate device code flow:**
   ```
   POST /oauth/device/code
   ```
   - Should return `device_code`, `user_code`, etc.
   - **Expected:** Audit event logged with `eventType: "auth"`, `action: "INITIATE"`

3. **Authorize device code:**
   ```
   POST /oauth/device/authorize
   Body: {
     "user_code": "<from step 2>",
     "username": "admin",
     "password": "password"
   }
   ```
   - **Expected:** Audit event logged with `eventType: "auth"`, `action: "AUTHORIZE"`, `status: "SUCCESS"`

4. **Poll for token:**
   ```
   POST /oauth/device/token
   Body: {
     "device_code": "<from step 2>"
   }
   ```
   - Should return `access_token` and `refresh_token`
   - **Expected:** Tokens are issued (no separate audit event for polling, but authorization completes)

5. **Verify audit events:**
   ```
   GET /api/admin/audit/events?eventType=auth
   Authorization: Bearer <token>
   ```
   - **Expected:** See events with:
     - `eventType: "auth"`
     - `toolName: "device_flow"`
     - `action: "INITIATE"` and `action: "AUTHORIZE"`
     - `userId: "1"` (or appropriate user ID)
     - `status: "SUCCESS"`

**✅ Success Criteria:**
- At least 2 audit events created (INITIATE, AUTHORIZE)
- Events contain correct user ID
- Events are timestamped correctly

---

## Test 2: Audit Logging - Search Requests

**Goal:** Verify that search requests are automatically logged via AOP.

### Steps:

1. **Perform a search (with valid token):**
   ```
   POST /api/search
   Authorization: Bearer <access_token>
   Body: {
     "query": "database performance",
     "mode": "semantic",
     "page": 0,
     "pageSize": 10
   }
   ```
   - Should return search results
   - **Expected:** Audit event logged automatically via AOP

2. **Query audit events for search:**
   ```
   GET /api/admin/audit/events?eventType=search
   Authorization: Bearer <token>
   ```
   - **Expected:** See event with:
     - `eventType: "search"`
     - `toolName: "search_docs"`
     - `action: "SEARCH"`
     - `status: "SUCCESS"`
     - `eventData` contains: `query`, `mode`, `resultCount`, `durationMs`

3. **Verify event data:**
   - Check that `eventData` JSON contains:
     ```json
     {
       "query": "database performance",
       "mode": "semantic",
       "page": 0,
       "pageSize": 10,
       "resultCount": <number>,
       "durationMs": <number>
     }
     ```

**✅ Success Criteria:**
- Search audit event is created automatically
- Event contains correct search parameters
- Event includes performance metrics (durationMs, resultCount)

---

## Test 3: Audit Logging - Token Refresh

**Goal:** Verify that token refresh operations are logged.

### Steps:

1. **Refresh access token:**
   ```
   POST /oauth/token
   Body: {
     "grant_type": "refresh_token",
     "refresh_token": "<refresh_token>"
   }
   ```
   - Should return new `access_token` and `refresh_token`
   - **Expected:** Audit event logged with `eventType: "token_op"`, `action: "REFRESH"`

2. **Query audit events for token operations:**
   ```
   GET /api/admin/audit/events?eventType=token_op
   Authorization: Bearer <token>
   ```
   - **Expected:** See event with:
     - `eventType: "token_op"`
     - `action: "REFRESH"`
     - `status: "SUCCESS"`
     - `userId: "1"` (or appropriate user ID)

**✅ Success Criteria:**
- Token refresh event is logged
- Event contains correct user ID
- Event status is "SUCCESS"

---

## Test 4: Token Revocation

**Goal:** Verify that token revocation works and is logged.

### Steps:

1. **Note current access token:**
   - Save the `access_token` from previous tests
   - Ensure it's still valid

2. **Revoke the token:**
   ```
   POST /oauth/revoke
   Authorization: Bearer <access_token>
   ```
   - **Expected:** Returns `200 OK` with message
   - **Expected:** Audit event logged with `eventType: "token_op"`, `action: "REVOKE"`

3. **Verify revocation worked:**
   ```
   POST /api/search
   Authorization: Bearer <revoked_token>
   Body: {
     "query": "test",
     "mode": "semantic"
   }
   ```
   - **Expected:** Returns `401 Unauthorized`
   - Token should no longer be valid

4. **Query audit events for revocation:**
   ```
   GET /api/admin/audit/events?eventType=token_op&status=SUCCESS
   Authorization: Bearer <new_token>
   ```
   - **Expected:** See revocation event with:
     - `eventType: "token_op"`
     - `action: "REVOKE"`
     - `status: "SUCCESS"`
     - `userId: "1"` (or appropriate user ID)

**✅ Success Criteria:**
- Token revocation returns 200 OK (RFC 7009 compliant)
- Revoked token no longer works for API calls
- Revocation event is logged in audit

---

## Test 5: Audit Query Endpoints

**Goal:** Verify that audit query endpoints work correctly.

### Steps:

1. **Get all audit events:**
   ```
   GET /api/admin/audit/events?page=0&size=20
   Authorization: Bearer <token>
   ```
   - **Expected:** Returns paginated list of audit events
   - Check pagination metadata: `totalElements`, `totalPages`, `currentPage`

2. **Filter by user ID:**
   ```
   GET /api/admin/audit/events?userId=1
   Authorization: Bearer <token>
   ```
   - **Expected:** Returns only events for user ID "1"

3. **Filter by event type:**
   ```
   GET /api/admin/audit/events?eventType=search
   Authorization: Bearer <token>
   ```
   - **Expected:** Returns only search events

4. **Filter by status:**
   ```
   GET /api/admin/audit/events?status=SUCCESS
   Authorization: Bearer <token>
   ```
   - **Expected:** Returns only successful events

5. **Get audit statistics:**
   ```
   GET /api/admin/audit/stats
   Authorization: Bearer <token>
   ```
   - **Expected:** Returns statistics:
     ```json
     {
       "totalEvents": <number>,
       "period": "last 24 hours",
       "byEventType": {
         "search": <count>,
         "auth": <count>,
         "token_op": <count>
       },
       "byStatus": {
         "SUCCESS": <count>,
         "FAILURE": <count>
       }
     }
     ```

**✅ Success Criteria:**
- All query endpoints return correct results
- Filters work as expected
- Statistics endpoint provides useful insights

---

## Test 6: Error Cases - Audit Logging

**Goal:** Verify that failures are logged correctly.

### Steps:

1. **Try search with invalid token:**
   ```
   POST /api/search
   Authorization: Bearer invalid_token
   Body: {
     "query": "test",
     "mode": "semantic"
   }
   ```
   - **Expected:** Returns `401 Unauthorized`
   - **Expected:** Audit event logged (if implemented in SecurityExceptionHandler)

2. **Try refresh with invalid refresh token:**
   ```
   POST /oauth/token
   Body: {
     "grant_type": "refresh_token",
     "refresh_token": "invalid_refresh_token"
   }
   ```
   - **Expected:** Returns `401 Unauthorized`
   - **Expected:** Audit event logged with `status: "FAILURE"`

3. **Query failed events:**
   ```
   GET /api/admin/audit/events?status=FAILURE
   Authorization: Bearer <valid_token>
   ```
   - **Expected:** See failure events with `errorMessage` populated

**✅ Success Criteria:**
- Failure events are logged
- Error messages are captured
- Events have correct status ("FAILURE")

---

## Test 7: End-to-End Flow

**Goal:** Test complete authentication → search → revocation → re-authentication flow.

### Steps:

1. **Start fresh** - Clear any existing tokens (delete `~/.search-answer-lab/tokens.json` if testing MCP)

2. **Authenticate:**
   - Follow device code flow (Steps 1-3 from Test 1)
   - Get valid `access_token` and `refresh_token`

3. **Perform searches:**
   - Make 2-3 search requests
   - Verify all are logged in audit

4. **Check audit stats:**
   - Call `/api/admin/audit/stats`
   - Verify counts include your searches and auth events

5. **Revoke token:**
   - Revoke the access token
   - Verify revocation is logged

6. **Verify token is invalid:**
   - Try to search with revoked token
   - Should get 401

7. **Re-authenticate:**
   - Go through device code flow again
   - Get new tokens

8. **Verify new token works:**
   - Perform search with new token
   - Should succeed

9. **Final audit check:**
   - Query all audit events
   - Verify complete history of your session:
     - Auth events (INITIATE, AUTHORIZE)
     - Search events
     - Token operations (REFRESH if applicable, REVOKE)
     - Re-authentication events

**✅ Success Criteria:**
- Complete flow works end-to-end
- All events are logged correctly
- Token lifecycle is properly managed
- Audit provides complete history

---

## Quick Test Checklist

Use this checklist for a quick verification:

- [ ] **Audit table exists** (check database or run migration)
- [ ] **Device code flow logs events** (INITIATE, AUTHORIZE)
- [ ] **Search requests log events** automatically
- [ ] **Token refresh logs events** (REFRESH)
- [ ] **Token revocation logs events** (REVOKE)
- [ ] **Revoked tokens don't work** (401 after revocation)
- [ ] **Audit query endpoints work** (`/api/admin/audit/events`, `/api/admin/audit/stats`)
- [ ] **Filters work** (userId, eventType, status)
- [ ] **Statistics are accurate**
- [ ] **Event data contains expected fields** (query, mode, durationMs, etc.)

---

## Troubleshooting

### Issue: No audit events created
- **Check:** Database migration `V12__create_audit_events_table.sql` ran successfully
- **Check:** Backend logs for errors when saving audit events
- **Check:** `AuditService` is being injected correctly

### Issue: Search events not logged
- **Check:** AOP is enabled (`@EnableAspectJAutoProxy` or Spring Boot auto-configuration)
- **Check:** `AuditAspect` is loaded as a Spring component
- **Check:** Search endpoint has `@PreAuthorize` annotation

### Issue: Query endpoints return 401
- **Check:** You're using a valid JWT token in Authorization header
- **Check:** Token has not expired
- **Check:** `/api/admin/**` is protected (requires authentication)

### Issue: Statistics empty
- **Check:** Statistics only show last 24 hours
- **Check:** Events were created within the last 24 hours
- **Check:** Query parameters are correct

---

## Expected Database State

After running all tests, check the database:

```sql
-- Check audit events table
SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT 20;

-- Count by event type
SELECT event_type, COUNT(*) FROM audit_events GROUP BY event_type;

-- Count by status
SELECT status, COUNT(*) FROM audit_events GROUP BY status;

-- Recent events
SELECT event_type, action, status, timestamp, user_id 
FROM audit_events 
ORDER BY timestamp DESC 
LIMIT 10;
```

---

## Next Steps After Testing

Once all tests pass:

1. ✅ Mark testing as complete in `STEP_7.md`
2. 📝 Document any issues found
3. 🔄 Fix any bugs discovered
4. 📊 Review audit event structure for production readiness
5. 🚀 Proceed to Step 7C (Token Refresh Testing) or Step 7D (Error Handling)

---

**Good luck with testing!** 🧪
