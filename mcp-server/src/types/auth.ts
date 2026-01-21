/**
 * Type definitions for OAuth Device Code Flow authentication.
 */

/**
 * Response from POST /oauth/device/code
 */
export interface DeviceCodeResponse {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number; // seconds
  interval: number; // polling interval in seconds
}

/**
 * Response from POST /oauth/device/token (polling) or POST /oauth/token (refresh)
 */
export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: string; // "Bearer"
  expires_in: number; // seconds
  scope: string; // space-separated scopes
}

/**
 * Tokens stored on disk in JSON format
 */
export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
  expiresAt: number; // Unix timestamp (seconds)
  scopes: string[];
  userId: string;
}

/**
 * Authentication error with context
 */
export class AuthError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly shouldRetry: boolean = false,
    public readonly deviceCodeInfo?: {
      userCode: string;
      verificationUri: string;
      expiresIn: number;
    }
  ) {
    super(message);
    this.name = "AuthError";
  }
}

/**
 * Authentication state for tracking current auth flow
 */
export interface AuthState {
  isAuthenticating: boolean;
  deviceCode?: string;
  userCode?: string;
  verificationUri?: string;
}
