/**
 * DeviceAuthManager handles OAuth Device Code Flow authentication.
 * Manages token lifecycle: initiation, polling, refresh, and storage.
 */

import { config } from "../config.js";
import { loadTokens, saveTokens, clearTokens } from "./TokenStorage.js";
import type {
  DeviceCodeResponse,
  TokenResponse,
  StoredTokens,
} from "../types/auth.js";
import { AuthError } from "../types/auth.js";

const TOKEN_REFRESH_BUFFER_MINUTES = 5; // Refresh token 5 minutes before expiry
const MAX_POLLING_DURATION_MS = 10 * 60 * 1000; // 10 minutes
const DEFAULT_POLLING_INTERVAL_MS = 5000; // 5 seconds

/**
 * DeviceAuthManager - Main class for OAuth Device Code Flow
 */
export class DeviceAuthManager {
  private authPromise: Promise<string> | null = null; // Lock for concurrent requests
  private pendingDeviceCode: {
    userCode: string;
    verificationUri: string;
    deviceCode: string;
    expiresAt: number; // Unix timestamp
  } | null = null;

  constructor(private readonly backendUrl: string) {}

  /**
   * Ensure we have a valid access token.
   * Main entry point - checks, refreshes, or initiates auth as needed.
   * @param nonBlocking If true, throws AuthError with device code info instead of blocking
   */
  async ensureAuthenticated(nonBlocking: boolean = false): Promise<string> {
    console.error("[DeviceAuthManager] ensureAuthenticated() called");
    // If already authenticating, wait for that to complete
    if (this.authPromise && !nonBlocking) {
      console.error("[DeviceAuthManager] Already authenticating, waiting...");
      return this.authPromise;
    }

    // Check for existing tokens
    console.error("[DeviceAuthManager] Checking for stored tokens...");
    const storedTokens = await loadTokens();
    if (storedTokens) {
      console.error(`[DeviceAuthManager] Found stored tokens, userId: ${storedTokens.userId}`);
      // Check if token is expired or near expiry
      if (this.isTokenExpired(storedTokens)) {
        console.error("[DeviceAuthManager] Token expired, attempting refresh...");
        // Try to refresh
        try {
          const refreshed = await this.refreshToken(storedTokens.refreshToken);
          if (refreshed) {
            console.error("[DeviceAuthManager] Token refreshed successfully");
            return refreshed.access_token;
          }
        } catch (error) {
          console.error(`[DeviceAuthManager] Token refresh failed: ${error}`);
          // If refresh failed (token revoked), clear local tokens
          // This ensures revoked tokens are not reused
          console.error("[DeviceAuthManager] Clearing local tokens due to refresh failure (token may be revoked)");
          await clearTokens();
          this.pendingDeviceCode = null;
          // Fall through to re-authentication
        }
      } else {
        // Token is valid (not expired)
        // Note: We don't check if token was revoked on backend here
        // because access tokens are stateless JWTs. They remain valid until expiry.
        // Revocation prevents new tokens, but current token works until expiry.
        console.error("[DeviceAuthManager] Token is valid, using stored token");
        return storedTokens.accessToken;
      }
    } else {
      console.error("[DeviceAuthManager] No stored tokens found");
    }

    // No valid token - initiate device flow
    if (nonBlocking) {
      // Check if we have a pending device code that's still valid
      if (this.pendingDeviceCode) {
        const now = Math.floor(Date.now() / 1000);
        const timeRemaining = this.pendingDeviceCode.expiresAt - now;
        
        if (timeRemaining > 0) {
          // Code is still valid - try to poll once to see if user has authorized
          console.error(`[DeviceAuthManager] Checking if pending device code ${this.pendingDeviceCode.userCode} has been authorized...`);
          try {
            const tokenResponse = await this.pollForTokenOnce(this.pendingDeviceCode.deviceCode);
            if (tokenResponse) {
              // User authorized! Save tokens and return
              const expiresAt = Math.floor(Date.now() / 1000) + tokenResponse.expires_in;
              const scopes = tokenResponse.scope.split(" ").filter((s: string) => s.length > 0);
              const userId = this.extractUserIdFromToken(tokenResponse.access_token);
              
              const storedTokens: StoredTokens = {
                accessToken: tokenResponse.access_token,
                refreshToken: tokenResponse.refresh_token,
                expiresAt,
                scopes,
                userId,
              };
              
              await saveTokens(storedTokens);
              this.pendingDeviceCode = null;
              console.error("✅ Authentication successful (pending device code authorized)!");
              return tokenResponse.access_token;
            }
          } catch (error: any) {
            // Check if error indicates code expired on backend
            if (error.message?.includes("expired") || error.message?.includes("invalid") || error.code === "expired_token") {
              console.error(`[DeviceAuthManager] Backend reports device code expired, clearing pending code`);
              this.pendingDeviceCode = null;
            } else {
              // Not authorized yet, that's fine - continue to return device code info
              console.error(`[DeviceAuthManager] Pending device code not yet authorized, returning code info`);
            }
          }
          
          // Reuse existing pending device code only if it's still valid
          const pending = this.pendingDeviceCode; // Store reference for type safety
          if (pending && timeRemaining > 0) {
            console.error(`[DeviceAuthManager] Reusing pending device code: ${pending.userCode} (${Math.floor(timeRemaining / 60)} min remaining)`);
            throw new AuthError(
              `Authentication required. Please visit ${pending.verificationUri} and enter code: ${pending.userCode}`,
              "authentication_required",
              true,
              {
                userCode: pending.userCode,
                verificationUri: pending.verificationUri,
                expiresIn: timeRemaining,
              }
            );
          } else {
            // Code expired or was cleared, generate new one
            console.error(`[DeviceAuthManager] Pending device code expired or invalid, generating new one`);
            this.pendingDeviceCode = null;
          }
        } else {
          // Pending device code expired, clear it
          console.error(`[DeviceAuthManager] Pending device code expired (${Math.abs(timeRemaining)} seconds ago), generating new one`);
          this.pendingDeviceCode = null;
        }
      }

      // Generate new device code and store it as pending
      const deviceCodeResponse = await this.requestDeviceCode();
      const expiresAt = Math.floor(Date.now() / 1000) + deviceCodeResponse.expires_in;
      
      this.pendingDeviceCode = {
        userCode: deviceCodeResponse.user_code,
        verificationUri: deviceCodeResponse.verification_uri,
        deviceCode: deviceCodeResponse.device_code,
        expiresAt,
      };

      console.error(`[DeviceAuthManager] Generated new device code: ${deviceCodeResponse.user_code}, expires at: ${new Date(expiresAt * 1000).toISOString()}`);

      throw new AuthError(
        `Authentication required. Please visit ${deviceCodeResponse.verification_uri} and enter code: ${deviceCodeResponse.user_code}`,
        "authentication_required",
        true,
        {
          userCode: deviceCodeResponse.user_code,
          verificationUri: deviceCodeResponse.verification_uri,
          expiresIn: deviceCodeResponse.expires_in,
        }
      );
    }

    console.error("[DeviceAuthManager] Initiating device code flow (blocking)...");
    this.authPromise = this.initiateDeviceFlow();
    try {
      const token = await this.authPromise;
      return token;
    } finally {
      this.authPromise = null;
    }
  }

  /**
   * Initiate device code flow and wait for user authorization.
   */
  private async initiateDeviceFlow(): Promise<string> {
    console.error("🔐 Authentication Required");
    console.error("");

    // Step 1: Request device code
    const deviceCodeResponse = await this.requestDeviceCode();
    const { device_code, user_code, verification_uri, interval } =
      deviceCodeResponse;

    // Step 2: Show prompt to user
    console.error(`Please visit: ${verification_uri}`);
    console.error(`Enter code: ${user_code}`);
    console.error("");
    console.error("Waiting for authorization...");

    // Step 3: Poll for token
    const tokenResponse = await this.pollForToken(
      device_code,
      interval * 1000 // Convert to milliseconds
    );
    
    // Clear pending device code since we got tokens
    if (this.pendingDeviceCode?.deviceCode === device_code) {
      this.pendingDeviceCode = null;
    }

    // Step 4: Save tokens
    const expiresAt = Math.floor(Date.now() / 1000) + tokenResponse.expires_in;
    const scopes = tokenResponse.scope.split(" ").filter((s) => s.length > 0);
    const userId = this.extractUserIdFromToken(tokenResponse.access_token);

    const storedTokens: StoredTokens = {
      accessToken: tokenResponse.access_token,
      refreshToken: tokenResponse.refresh_token,
      expiresAt,
      scopes,
      userId,
    };

    await saveTokens(storedTokens);
    console.error("✅ Authentication successful!");
    
    // Clear pending device code since we're now authenticated
    this.pendingDeviceCode = null;

    return tokenResponse.access_token;
  }

  /**
   * Poll for token once (single attempt).
   * Returns TokenResponse if authorized, null if still pending.
   */
  private async pollForTokenOnce(deviceCode: string): Promise<TokenResponse | null> {
    try {
      const response = await fetch(`${this.backendUrl}/oauth/device/token`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ device_code: deviceCode }),
      });

      if (response.ok) {
        return (await response.json()) as TokenResponse;
      }

      const errorData = (await response.json()) as {
        error?: string;
        error_description?: string;
      };
      
      if (errorData.error === "authorization_pending") {
        // Still waiting - return null
        return null;
      }

      // Check if code expired
      if (errorData.error === "expired_token" || 
          errorData.error_description?.toLowerCase().includes("expired") ||
          errorData.error_description?.toLowerCase().includes("expire")) {
        // Code expired - throw with special code
        throw new AuthError(
          `Device code expired: ${errorData.error_description || errorData.error || "Unknown error"}`,
          "expired_token",
          false
        );
      }

      // Other error - throw
      throw new AuthError(
        `Token polling failed: ${errorData.error_description || errorData.error || "Unknown error"}`,
        errorData.error || "token_poll_failed",
        false
      );
    } catch (error: any) {
      if (error instanceof AuthError) {
        throw error;
      }
      // Network error - assume not authorized yet
      return null;
    }
  }

  /**
   * Request device code from backend.
   */
  private async requestDeviceCode(): Promise<DeviceCodeResponse> {
    const url = `${this.backendUrl}/oauth/device/code`;

    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new AuthError(
          `Failed to initiate device flow: ${response.status} ${errorText}`,
          "device_code_request_failed",
          true
        );
      }

      return (await response.json()) as DeviceCodeResponse;
    } catch (error: any) {
      if (error instanceof AuthError) {
        throw error;
      }
      throw new AuthError(
        `Network error during device code request: ${error.message}`,
        "network_error",
        true
      );
    }
  }

  /**
   * Poll for token until user authorizes or timeout.
   * Also checks pending device code if deviceCode is not provided.
   */
  private async pollForToken(
    deviceCode?: string,
    intervalMs?: number
  ): Promise<TokenResponse> {
    // If deviceCode not provided, try to use pending device code
    if (!deviceCode && this.pendingDeviceCode) {
      deviceCode = this.pendingDeviceCode.deviceCode;
      // Default interval from backend is usually 5 seconds
      intervalMs = intervalMs || 5000;
      console.error(`[DeviceAuthManager] Polling with pending device code: ${this.pendingDeviceCode.userCode}`);
    }
    
    if (!deviceCode) {
      throw new AuthError(
        "No device code available for polling",
        "no_device_code",
        false
      );
    }
    
    if (!intervalMs) {
      intervalMs = 5000; // Default 5 seconds
    }
    const startTime = Date.now();
    const pollingInterval = Math.max(intervalMs, DEFAULT_POLLING_INTERVAL_MS);

    while (true) {
      // Check timeout
      if (Date.now() - startTime > MAX_POLLING_DURATION_MS) {
        throw new AuthError(
          "Device code authorization timed out. Please try again.",
          "authorization_timeout",
          false
        );
      }

      try {
        const response = await fetch(`${this.backendUrl}/oauth/device/token`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ device_code: deviceCode }),
        });

        if (response.ok) {
          return (await response.json()) as TokenResponse;
        }

        const errorData = (await response.json()) as {
          error?: string;
          error_description?: string;
        };
        if (errorData.error === "authorization_pending") {
          // Still waiting - continue polling
          await this.sleep(pollingInterval);
          continue;
        }

        // Other error
        throw new AuthError(
          `Token polling failed: ${errorData.error_description || errorData.error || "Unknown error"}`,
          errorData.error || "token_poll_failed",
          false
        );
      } catch (error: any) {
        if (error instanceof AuthError) {
          throw error;
        }
        // Network error - retry after interval
        await this.sleep(pollingInterval);
      }
    }
  }

  /**
   * Refresh access token using refresh token.
   */
  async refreshToken(refreshToken: string): Promise<TokenResponse | null> {
    try {
      const response = await fetch(`${this.backendUrl}/oauth/token`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          grant_type: "refresh_token",
          refresh_token: refreshToken,
        }),
      });

      if (!response.ok) {
        const errorData = (await response.json()) as {
          error?: string;
          error_description?: string;
        };
        
        // If refresh failed (401/403 or invalid_grant), token was likely revoked or expired
        // Clear local tokens to prevent reuse
        if (response.status === 401 || response.status === 403 || errorData.error === "invalid_grant") {
          console.error("[DeviceAuthManager] Refresh failed with 401/403/invalid_grant - token likely revoked or expired, clearing local tokens");
          await clearTokens();
          this.pendingDeviceCode = null;
          
          if (errorData.error === "invalid_grant") {
            // Refresh token expired - need to re-authenticate
            return null;
          }
        }
        
        throw new AuthError(
          `Token refresh failed: ${errorData.error_description || errorData.error || "Unknown error"}`,
          errorData.error || "refresh_failed",
          false
        );
      }

      const tokenResponse = (await response.json()) as TokenResponse;

      // Save new tokens
      const expiresAt =
        Math.floor(Date.now() / 1000) + tokenResponse.expires_in;
      const scopes = tokenResponse.scope.split(" ").filter((s) => s.length > 0);
      const userId = this.extractUserIdFromToken(tokenResponse.access_token);

      const storedTokens: StoredTokens = {
        accessToken: tokenResponse.access_token,
        refreshToken: tokenResponse.refresh_token,
        expiresAt,
        scopes,
        userId,
      };

      await saveTokens(storedTokens);
      return tokenResponse;
    } catch (error: any) {
      if (error instanceof AuthError) {
        throw error;
      }
      throw new AuthError(
        `Network error during token refresh: ${error.message}`,
        "network_error",
        true
      );
    }
  }

  /**
   * Check if token is expired (with buffer).
   */
  private isTokenExpired(tokens: StoredTokens): boolean {
    const bufferSeconds = TOKEN_REFRESH_BUFFER_MINUTES * 60;
    const expiryTime = tokens.expiresAt - bufferSeconds;
    const now = Math.floor(Date.now() / 1000);
    return now >= expiryTime;
  }

  /**
   * Extract user ID from JWT token (basic parsing).
   * Note: This is a simple extraction - full validation happens on backend.
   */
  private extractUserIdFromToken(token: string): string {
    try {
      const parts = token.split(".");
      if (parts.length !== 3) {
        return "unknown";
      }
      const payload = JSON.parse(
        Buffer.from(parts[1], "base64").toString("utf-8")
      );
      return payload.sub || "unknown";
    } catch {
      return "unknown";
    }
  }

  /**
   * Sleep utility for polling.
   */
  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /**
   * Get current access token (if valid) or null.
   * Does not trigger authentication flow.
   */
  async getAccessToken(): Promise<string | null> {
    const storedTokens = await loadTokens();
    if (!storedTokens) {
      return null;
    }

    if (this.isTokenExpired(storedTokens)) {
      // Try to refresh
      try {
        const refreshed = await this.refreshToken(storedTokens.refreshToken);
        return refreshed ? refreshed.access_token : null;
      } catch (error) {
        // If refresh failed (token revoked), clear local tokens
        console.error(`[DeviceAuthManager] Refresh failed in getAccessToken, clearing tokens: ${error}`);
        await clearTokens();
        this.pendingDeviceCode = null;
        return null;
      }
    }

    return storedTokens.accessToken;
  }

  /**
   * Clear stored tokens (logout).
   * Only clears local tokens, does not revoke on server.
   */
  async logout(): Promise<void> {
    await clearTokens();
    this.pendingDeviceCode = null;
  }

  /**
   * Revoke access tokens (server-side revocation + local cleanup).
   * Calls backend revocation endpoint and clears local tokens.
   */
  async revokeAccess(): Promise<void> {
    console.error("[DeviceAuthManager] Revoking access...");
    
    try {
      // Get current tokens
      const storedTokens = await loadTokens();
      
      if (storedTokens && storedTokens.accessToken) {
        // Try to revoke on server-side (best effort)
        try {
          const response = await fetch(`${this.backendUrl}/oauth/revoke`, {
            method: "POST",
            headers: {
              "Authorization": `Bearer ${storedTokens.accessToken}`,
              "Content-Type": "application/json",
            },
          });
          
          if (response.ok) {
            console.error("[DeviceAuthManager] ✅ Token revoked on server");
          } else {
            console.error(`[DeviceAuthManager] ⚠️ Server revocation returned ${response.status}, continuing with local cleanup`);
          }
        } catch (error: any) {
          // Continue with local cleanup even if server request fails
          console.error(`[DeviceAuthManager] ⚠️ Server revocation failed: ${error.message}, continuing with local cleanup`);
        }
      }
      
      // Always clear local tokens
      await clearTokens();
      this.pendingDeviceCode = null;
      
      console.error("[DeviceAuthManager] ✅ Access revoked - local tokens cleared");
    } catch (error: any) {
      console.error(`[DeviceAuthManager] ❌ Error during revocation: ${error.message}`);
      // Still try to clear local tokens
      try {
        await clearTokens();
        this.pendingDeviceCode = null;
      } catch (clearError: any) {
        console.error(`[DeviceAuthManager] ❌ Failed to clear tokens: ${clearError.message}`);
      }
      throw error;
    }
  }
}
