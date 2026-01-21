# Step 6: Authorization Page & Tool Response Fix

**Goal:** Make authentication visible and user-friendly  
**Estimated Time:** ~2 hours (1 hour tool response fix + 1 hour authorization page)  
**Impact:** HIGH - Transforms unusable UX into smooth experience  
**Status:** ✅ Detailed plan complete - Ready for implementation  
**Last Reviewed:** January 21, 2025

## 📋 Overview

This step fixes the critical UX issue where Claude Desktop shows "There was an error" instead of displaying authentication instructions. It implements two complementary solutions:

1. **Part 1:** Fix MCP tool response to return instructions instead of errors
2. **Part 2:** Build a professional authorization page with pre-filled device codes

Together, these changes transform the authentication experience from "hidden error → check logs" to "clear instructions → click link → authorize → done".

## 🎯 Implementation Strategy

**Approach:** Minimal changes to existing code
- **Part 1:** Update tool handler to return formatted message instead of throwing error (NO changes to DeviceAuthManager needed)
- **Part 2:** Add new GET endpoint and HTML template (doesn't affect existing JSON endpoint)

**Why This Approach:**
- Less risky (doesn't change core auth logic)
- Backward compatible (Postman/testing still works)
- Easier to implement and test
- Can be done incrementally

---

## What We're Fixing

### Current Problem (Bad UX)

```
User: "Search for database performance"
   ↓
MCP server needs auth
   ↓
Throws error
   ↓
Claude Desktop: "There was an error"
   ↓
User: "WTF? What error?"
   ↓
User must: Check MCP logs, find URL, copy code, paste manually
```

### After Step 6 (Good UX)

```
User: "Search for database performance"
   ↓
MCP server needs auth
   ↓
Returns instructions (NOT error)
   ↓
Claude Desktop: "I need authorization. Please visit:
                 http://localhost:8080/oauth/device/authorize?code=WXYZ-5678
                 
                 Once you've authorized, ask me to search again."
   ↓
User: Clicks link
   ↓
Browser: Opens pre-filled authorization form
   ↓
User: Clicks "Authorize"
   ↓
Page: "✅ Success! Go back to Claude and search again."
   ↓
User: "Search for database performance"
   ↓
Claude: [Returns search results]
```

---

## ⚠️ Important: Current Implementation Context

**Current Code Structure:**
- `DeviceAuthManager.ensureAuthenticated(nonBlocking: boolean)` currently throws `AuthError` when auth needed
- `AuthError` contains `deviceCodeInfo: { userCode, verificationUri, expiresIn }`
- `searchDocs.ts` handler catches `AuthError` and re-throws as generic `Error`
- This causes Claude Desktop to show "There was an error" instead of instructions

**What We're Changing:**
- **Simple approach:** Update tool handler to return formatted message instead of re-throwing error
- **NO changes needed to DeviceAuthManager** - it continues to throw `AuthError` as before
- Tool handler extracts `deviceCodeInfo` and returns formatted message
- This makes instructions visible in Claude Desktop without changing core auth logic

**Files to Modify:**
1. `mcp-server/src/tools/searchDocs.ts` - Return message instead of throwing (1 change)
2. `backend/src/main/java/.../controller/DeviceAuthPageController.java` - NEW file
3. `backend/src/main/resources/templates/device-authorize.html` - NEW file
4. `backend/src/main/java/.../controller/DeviceAuthController.java` - Add form support (optional)
5. `backend/pom.xml` - Add Thymeleaf dependency

---

## Part 1: Fix MCP Tool Response (TypeScript)

### 1.1 Add AuthResult Type Definition

**File:** `mcp-server/src/types/auth.ts`

**Add new interface at the end:**

```typescript
// BEFORE (Current - BAD)
private async initiateDeviceFlow() {
  // ... device flow logic ...
  
  console.error("\n==============================================");
  console.error("AUTHENTICATION REQUIRED");
  console.error("==============================================");
  console.error(`Visit: ${verification_uri}`);
  console.error(`Enter code: ${user_code}`);
  console.error("==============================================\n");
  
  // This throws an error that Claude Desktop hides
  throw new Error("Authentication required");
}
```

**Change to return structured data instead:**

```typescript
// AFTER (New - GOOD)
private async initiateDeviceFlow(): Promise<AuthResult> {
  const response = await fetch(`${this.backendUrl}/oauth/device/code`, {
    method: "POST",
    headers: { "Content-Type": "application/json" }
  });
  
  const data = await response.json();
  const { device_code, user_code, verification_uri, expires_in } = data;
  
  // Store device code for polling
  this.pendingDeviceCode = device_code;
  
  // Return auth instructions (NOT throw error)
  return {
    needsAuth: true,
    userCode: user_code,
    verificationUri: verification_uri,
    expiresIn: expires_in,
    message: this.formatAuthMessage(user_code, verification_uri, expires_in)
  };
}

private formatAuthMessage(userCode: string, verificationUri: string, expiresIn: number): string {
  // Add code as query param for pre-filling
  const urlWithCode = `${verificationUri}?code=${userCode}`;
  
  return `I need authorization to search your knowledge base.

Please follow these steps:

1. Visit this link: ${urlWithCode}
2. Review the permissions (search documents, read content)
3. Click "Authorize"

This code will expire in ${Math.floor(expiresIn / 60)} minutes.

Once you've authorized, ask me to search again and I'll use the stored credentials.`;
}
```

**Add AuthResult type:**

```typescript
// Add at top of file
interface AuthResult {
  needsAuth: boolean;
  userCode?: string;
  verificationUri?: string;
  expiresIn?: number;
  message?: string;
  token?: string;
}
```

### 1.3 Update Tool Handler to Return Instructions Instead of Error

**File:** `mcp-server/src/tools/searchDocs.ts`

**Current Code (lines 99-111):**
```typescript
} catch (authError: any) {
  console.error(`[AUTH] Authentication error: ${authError.message}`, authError);
  if (authError instanceof AuthError) {
    // If device code info is provided, return user-friendly message as a formatted error
    if (authError.deviceCodeInfo) {
      const { userCode, verificationUri } = authError.deviceCodeInfo;
      const authMessage = 
        `🔐 AUTHENTICATION REQUIRED\n\n` +
        `To search the knowledge base, please authenticate first:\n\n` +
        `📍 Visit: ${verificationUri}\n` +
        `🔑 Enter code: ${userCode}\n\n` +
        `⏳ After authenticating, try the search again.\n\n` +
        `(The device code expires in 10 minutes)`;
      
      // Throw error with a very clear message that Claude Desktop will show
      const error = new Error(authMessage);
      (error as any).code = "AUTHENTICATION_REQUIRED";
      throw error;
    }
```

**Change to:**

```typescript
} catch (authError: any) {
  console.error(`[AUTH] Authentication error: ${authError.message}`, authError);
  if (authError instanceof AuthError) {
    // If device code info is provided, return instructions as tool result (NOT error)
    if (authError.deviceCodeInfo) {
      const { userCode, verificationUri } = authError.deviceCodeInfo;
      
      // Format URL with code as query parameter for pre-filling
      const urlWithCode = `${verificationUri}?code=${userCode}`;
      
      const authMessage = 
        `I need authorization to search your knowledge base.\n\n` +
        `Please follow these steps:\n\n` +
        `1. Visit this link: ${urlWithCode}\n` +
        `2. Review the permissions shown\n` +
        `3. Click "Authorize"\n\n` +
        `This code expires in 10 minutes.\n\n` +
        `Once you've authorized, ask me to search again and I'll use the stored credentials.`;
      
      // Return as successful tool response instead of throwing error
      // This makes Claude Desktop show the full message
      return {
        content: [{
          type: "text",
          text: authMessage
        }]
      };
    }
```

**Important Notes:**
- The current code throws an `Error`, which Claude Desktop may hide or truncate
- By returning a tool result (not throwing), Claude Desktop will display it fully
- The message format is designed to be clear and actionable
- URL includes `?code=` parameter so authorization page can pre-fill

---

## Part 2: Build Authorization Page (Backend)

### 2.1 Add Thymeleaf Dependency

**File:** `backend/pom.xml`

**Add inside `<dependencies>`:**

```xml
<!-- Thymeleaf for HTML templates -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

**Reload Maven:**
```bash
cd backend
./mvnw clean install
```

### 2.2 Create Authorization Page Template

**File:** `backend/src/main/resources/templates/device-authorize.html`

**Create this HTML template:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Authorize MCP Access</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        
        .container {
            background: white;
            border-radius: 16px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            max-width: 500px;
            width: 100%;
            padding: 40px;
        }
        
        .logo {
            text-align: center;
            margin-bottom: 30px;
        }
        
        .logo svg {
            width: 64px;
            height: 64px;
            fill: #667eea;
        }
        
        h1 {
            font-size: 28px;
            color: #1a202c;
            margin-bottom: 10px;
            text-align: center;
        }
        
        .subtitle {
            color: #718096;
            text-align: center;
            margin-bottom: 30px;
            font-size: 14px;
        }
        
        .auth-form {
            display: flex;
            flex-direction: column;
            gap: 20px;
        }
        
        .form-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        
        label {
            font-weight: 600;
            color: #2d3748;
            font-size: 14px;
        }
        
        input[type="text"] {
            padding: 12px 16px;
            border: 2px solid #e2e8f0;
            border-radius: 8px;
            font-size: 18px;
            letter-spacing: 2px;
            text-align: center;
            font-family: 'Courier New', monospace;
            font-weight: bold;
            color: #2d3748;
            transition: border-color 0.2s;
        }
        
        input[type="text"]:focus {
            outline: none;
            border-color: #667eea;
        }
        
        input[type="text"]:read-only {
            background: #f7fafc;
            color: #667eea;
        }
        
        .permissions {
            background: #f7fafc;
            border-radius: 8px;
            padding: 16px;
            border-left: 4px solid #667eea;
        }
        
        .permissions h3 {
            font-size: 14px;
            color: #2d3748;
            margin-bottom: 12px;
            font-weight: 600;
        }
        
        .permissions ul {
            list-style: none;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        
        .permissions li {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #4a5568;
            font-size: 14px;
        }
        
        .permissions li::before {
            content: "✓";
            color: #48bb78;
            font-weight: bold;
            font-size: 16px;
        }
        
        .timer {
            background: #fff5f5;
            border: 1px solid #feb2b2;
            border-radius: 8px;
            padding: 12px;
            text-align: center;
            color: #c53030;
            font-size: 13px;
        }
        
        .timer strong {
            font-weight: 600;
        }
        
        .button-group {
            display: flex;
            gap: 12px;
            margin-top: 10px;
        }
        
        button {
            flex: 1;
            padding: 14px 24px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        
        .btn-primary {
            background: #667eea;
            color: white;
        }
        
        .btn-primary:hover {
            background: #5568d3;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
        
        .btn-secondary {
            background: #e2e8f0;
            color: #4a5568;
        }
        
        .btn-secondary:hover {
            background: #cbd5e0;
        }
        
        .success-message {
            background: #f0fff4;
            border: 2px solid #48bb78;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            display: none;
        }
        
        .success-message.show {
            display: block;
        }
        
        .success-icon {
            font-size: 48px;
            margin-bottom: 12px;
        }
        
        .success-message h2 {
            color: #22543d;
            margin-bottom: 8px;
            font-size: 20px;
        }
        
        .success-message p {
            color: #2f855a;
            font-size: 14px;
        }
        
        .error-message {
            background: #fff5f5;
            border: 2px solid #fc8181;
            border-radius: 8px;
            padding: 16px;
            color: #c53030;
            display: none;
        }
        
        .error-message.show {
            display: block;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
            </svg>
        </div>
        
        <h1>Authorize MCP Access</h1>
        <p class="subtitle">Search Answer Lab requests access to your documents</p>
        
        <div class="success-message" id="successMessage">
            <div class="success-icon">✅</div>
            <h2>Authorization Successful!</h2>
            <p>You can now return to Claude Desktop and continue your search.</p>
        </div>
        
        <div class="error-message" id="errorMessage"></div>
        
        <form class="auth-form" id="authForm">
            <div class="form-group">
                <label for="userCode">Enter Device Code</label>
                <input 
                    type="text" 
                    id="userCode" 
                    name="userCode" 
                    placeholder="XXXX-XXXX"
                    th:value="${code}"
                    maxlength="9"
                    pattern="[A-Z0-9]{4}-[A-Z0-9]{4}"
                    required
                    autofocus
                />
            </div>
            
            <div class="permissions">
                <h3>This will grant access to:</h3>
                <ul>
                    <li>Search your technical documents</li>
                    <li>Read document content</li>
                    <li>View search results</li>
                </ul>
            </div>
            
            <div class="timer" id="timer">
                Code expires in <strong id="timeRemaining">10:00</strong>
            </div>
            
            <div class="button-group">
                <button type="button" class="btn-secondary" onclick="window.close()">Cancel</button>
                <button type="submit" class="btn-primary">Authorize</button>
            </div>
        </form>
    </div>
    
    <script>
        // Auto-format device code input
        const userCodeInput = document.getElementById('userCode');
        userCodeInput.addEventListener('input', function(e) {
            let value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
            if (value.length > 4) {
                value = value.slice(0, 4) + '-' + value.slice(4, 8);
            }
            e.target.value = value;
        });
        
        // Countdown timer (10 minutes)
        let seconds = 600;
        const timerElement = document.getElementById('timeRemaining');
        
        function updateTimer() {
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            timerElement.textContent = `${mins}:${secs.toString().padStart(2, '0')}`;
            
            if (seconds > 0) {
                seconds--;
            } else {
                document.getElementById('timer').innerHTML = '<strong>Code expired! Please request a new code.</strong>';
            }
        }
        
        setInterval(updateTimer, 1000);
        updateTimer();
        
        // Handle form submission
        document.getElementById('authForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const userCode = document.getElementById('userCode').value;
            const errorMessage = document.getElementById('errorMessage');
            const successMessage = document.getElementById('successMessage');
            
            errorMessage.classList.remove('show');
            successMessage.classList.remove('show');
            
            try {
                const response = await fetch('/oauth/device/authorize', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `userCode=${encodeURIComponent(userCode)}`
                });
                
                if (response.ok) {
                    // Success!
                    document.getElementById('authForm').style.display = 'none';
                    successMessage.classList.add('show');
                } else {
                    const error = await response.text();
                    errorMessage.textContent = error || 'Invalid or expired code. Please try again.';
                    errorMessage.classList.add('show');
                }
            } catch (error) {
                errorMessage.textContent = 'Network error. Please check your connection and try again.';
                errorMessage.classList.add('show');
            }
        });
    </script>
</body>
</html>
```

### 2.3 Create Controller for Authorization Page

**File:** `backend/src/main/java/com/searchlab/controller/DeviceAuthPageController.java`

**Create new controller:**

```java
package com.searchlab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for device authorization HTML page
 */
@Controller
public class DeviceAuthPageController {

    @GetMapping("/oauth/device/authorize")
    public String showAuthorizePage(
            @RequestParam(required = false) String code,
            Model model
    ) {
        // Pre-fill code if provided in URL
        model.addAttribute("code", code != null ? code : "");
        return "device-authorize";
    }
}
```

### 2.4 Update DeviceAuthController to Accept Form Data

**File:** `backend/src/main/java/com/searchlab/controller/DeviceAuthController.java`

**Current Implementation:**
- `POST /oauth/device/authorize` expects JSON body: `{ "user_code": "...", "username": "...", "password": "..." }`

**Required Change:**
- Accept both JSON (for Postman) and form-urlencoded (for HTML form)
- Form submission sends: `user_code=XXXX-XXXX&username=admin&password=password`

**Option 1: Add separate endpoint (Cleaner)**
```java
@PostMapping(value = "/oauth/device/authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public ResponseEntity<?> authorizeDeviceFromForm(
        @RequestParam String userCode,
        @RequestParam String username,
        @RequestParam String password
) {
    // Same logic as existing method, but accepts form params
    return authorizeDeviceCode(userCode, username, password);
}
```

**Option 2: Update existing method to accept both (Simpler)**
```java
@PostMapping(value = "/oauth/device/authorize", 
             consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public ResponseEntity<Map<String, Object>> authorizeDeviceCode(
        @RequestBody(required = false) Map<String, String> jsonBody,
        @RequestParam(required = false) String user_code,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String password
) {
    // Extract from either source
    String userCode = jsonBody != null ? jsonBody.get("user_code") : user_code;
    String user = jsonBody != null ? jsonBody.get("username") : username;
    String pass = jsonBody != null ? jsonBody.get("password") : password;
    
    // Rest of existing logic...
}
```

**Recommendation:** Option 1 is cleaner and maintains backward compatibility. The existing JSON endpoint continues to work for Postman/testing.

### 2.5 Update Security Config

**File:** `backend/src/main/java/com/searchlab/config/SecurityConfig.java`

**Ensure authorization page endpoints are public:**

**Check current config:**
- `GET /oauth/device/authorize` should be public (to show HTML page)
- `POST /oauth/device/authorize` should be public (to accept form submission)
- CSRF might need to be disabled for `/oauth/**` endpoints OR configure CSRF token for form

**If CSRF is enabled:**
- Option A: Disable CSRF for `/oauth/**` endpoints (simpler for dev)
- Option B: Add CSRF token to form (proper for production)

**For development (recommended):**
```java
http.csrf(csrf -> csrf
    .ignoringRequestMatchers("/oauth/**")  // Disable CSRF for OAuth endpoints
);
```

**Current config should already allow `/oauth/**` to be public based on Phase 1 implementation, but verify:**
- `GET /oauth/device/authorize` is accessible (no auth required)
- `POST /oauth/device/authorize` is accessible (no auth required)

---

## Part 3: Testing Step 6

### 4.1 Rebuild and Restart

**Terminal 1 - Backend:**
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

**Terminal 2 - MCP Server:**
```bash
cd mcp-server
npm run build
npm start
```

### 4.2 Test the Flow

**In Claude Desktop:**

```
You: "Search for database performance"

Expected Response (First Time):
Claude: "I need authorization to search your knowledge base.

Please follow these steps:

1. Visit this link: http://localhost:8080/oauth/device/authorize?code=WXYZ-5678
2. Review the permissions (search documents, read content)
3. Click "Authorize"

This code will expire in 10 minutes.

Once you've authorized, ask me to search again and I'll use the stored credentials."
```

**Click the link:**
- Browser opens
- Code is pre-filled
- Shows permissions clearly
- Click "Authorize"
- See success message

**Go back to Claude:**

```
You: "Search for database performance"

Expected Response (Second Time):
Claude: "Found 3 results:

1. **Database Indexing Strategies**
   Database indexes improve query performance...
   Score: 0.862

2. **Query Optimization Techniques**
   Use EXPLAIN to analyze...
   Score: 0.754

3. **Connection Pooling Best Practices**
   Reuse connections to reduce overhead...
   Score: 0.691"
```

### 4.3 Verification Checklist

After testing, verify:

- [ ] Claude shows full auth instructions (not "There was an error")
- [ ] Authorization URL includes `?code=XXXX-XXXX`
- [ ] Authorization page opens in browser
- [ ] Code is pre-filled in the form
- [ ] Timer counts down from 10:00
- [ ] Click "Authorize" shows success message
- [ ] Second search returns results without auth prompt
- [ ] Authorization page looks professional

---

## Common Issues & Solutions

### Issue 1: "There was an error" Still Appears

**Cause:** MCP tool handler still throwing error

**Solution:** Make sure you updated the tool handler to return `authResult.message` instead of throwing

### Issue 2: Code Not Pre-filled

**Cause:** URL doesn't include `?code=` parameter

**Solution:** Check `formatAuthMessage()` method appends code to URL

### Issue 3: Authorization Page Not Found

**Cause:** Thymeleaf dependency missing or template not in correct location

**Solution:** 
- Verify `spring-boot-starter-thymeleaf` in pom.xml
- Template must be at `src/main/resources/templates/device-authorize.html`
- Restart backend after adding dependency

### Issue 4: "Invalid Code" Error

**Cause:** Device code expired or wrong format

**Solution:**
- Check device code is 4-4 format (XXXX-XXXX)
- Codes expire in 10 minutes
- Request new code if expired

### Issue 5: CSS Not Loading

**Cause:** Inline CSS should work, but might be cached

**Solution:** Hard refresh browser (Ctrl+Shift+R / Cmd+Shift+R)

---

## What You've Accomplished

After completing Step 6:

✅ **User sees clear auth instructions in Claude**
- No more "There was an error"
- Full message with URL and code visible
- Natural conversation flow

✅ **Professional authorization page**
- Clean, modern UI
- Pre-filled code
- Shows what permissions are granted
- Countdown timer
- Success confirmation

✅ **Smooth user experience**
- Click link → Authorize → Search works
- No manual copy-pasting
- No checking logs
- Works like a real OAuth flow

---

## Next Steps

**After Step 6, you should:**

1. **Test the complete flow** (auth → search → works)
2. **Take screenshots** (for documentation/blog)
3. **Move to Step 7** (Audit logging - optional but recommended)

**Or skip to testing:**
- Token expiry and refresh
- Revocation flow
- Multiple users

---

## Success Criteria

Step 6 is complete when:

- [ ] User sees auth instructions in Claude (not error message)
- [ ] Authorization URL includes pre-filled code
- [ ] Authorization page renders correctly
- [ ] Can authorize by clicking button
- [ ] Success message appears after authorization
- [ ] Subsequent searches work without re-auth
- [ ] UX feels smooth and professional

---

---

## Part 5: Implementation Order & Summary

### Recommended Implementation Order

**Step 1: Fix Tool Response (30 minutes)**
1. Update `searchDocs.ts` to return formatted message instead of throwing error
2. Format URL with `?code=` parameter
3. Test: Verify Claude Desktop shows full instructions

**Step 2: Create Authorization Page (1 hour)**
1. Add Thymeleaf dependency
2. Create `DeviceAuthPageController.java`
3. Create `device-authorize.html` template
4. Update POST endpoint to accept form data
5. Test: Verify page loads with pre-filled code

**Step 3: Integration Testing (30 minutes)**
1. Test complete flow end-to-end
2. Verify all edge cases
3. Check error handling

**Total Time:** ~2 hours

### Key Changes Summary

| Component | Current Behavior | New Behavior |
|-----------|-----------------|--------------|
| Tool Handler | Throws error → Claude shows "error" | Returns message → Claude shows instructions |
| Auth URL | `http://localhost:8080/oauth/device/authorize` | `http://localhost:8080/oauth/device/authorize?code=XXXX-XXXX` |
| Device Code Entry | Manual copy-paste from logs | Pre-filled in HTML form |
| User Experience | Hidden error, check logs | Clear instructions in Claude chat |

---

## 📚 Quick Reference: File Changes Summary

### Files to Create
1. `backend/src/main/java/com/searchlab/controller/DeviceAuthPageController.java`
2. `backend/src/main/resources/templates/device-authorize.html`

### Files to Modify
1. `mcp-server/src/tools/searchDocs.ts` - Change error throwing to message return
2. `backend/src/main/java/com/searchlab/controller/DeviceAuthController.java` - Add form support (optional)
3. `backend/pom.xml` - Add Thymeleaf dependency

### Files NOT to Modify
- `mcp-server/src/auth/DeviceAuthManager.ts` - No changes needed (continues throwing AuthError)
- `mcp-server/src/types/auth.ts` - No changes needed (AuthError already has deviceCodeInfo)

---

**End of Step 6 Implementation Plan**  
**Status:** ✅ Detailed plan complete - Ready to implement  
**Next:** Implement Step 6, then move to Step 7 (Audit Logging)

**Estimated completion time:** 2 hours  
**Complexity:** ⭐⭐⭐☆☆ (Medium)

**Last Updated:** January 21, 2025  
**Reviewed Against:** Current implementation (Phase 1 & 2 complete)
