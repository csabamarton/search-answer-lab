/**
 * Environment configuration for MCP server.
 */

export interface Config {
  backendUrl: string;
  port: number;
  requestTimeoutMs: number;
}

/**
 * Validates that a string is a valid URL (basic validation).
 */
function isValidUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Normalizes URL by removing trailing slash.
 */
function normalizeUrl(url: string): string {
  return url.replace(/\/+$/, "");
}

/**
 * Reads and validates configuration from environment variables.
 */
export function loadConfig(): Config {
  const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
  const port = parseInt(process.env.PORT || "3001", 10);
  const requestTimeoutMs = parseInt(process.env.REQUEST_TIMEOUT_MS || "8000", 10);

  const normalizedBackendUrl = normalizeUrl(backendUrl);

  if (!isValidUrl(normalizedBackendUrl)) {
    throw new Error(`Invalid BACKEND_URL: ${backendUrl}`);
  }

  if (isNaN(port) || port < 1 || port > 65535) {
    throw new Error(`Invalid PORT: ${process.env.PORT}`);
  }

  if (isNaN(requestTimeoutMs) || requestTimeoutMs < 1) {
    throw new Error(`Invalid REQUEST_TIMEOUT_MS: ${process.env.REQUEST_TIMEOUT_MS}`);
  }

  return {
    backendUrl: normalizedBackendUrl,
    port,
    requestTimeoutMs,
  };
}

export const config = loadConfig();