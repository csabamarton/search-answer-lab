/**
 * MCP tool handler for search_docs.
 * Validates input and forwards to backend client.
 */

import { search as callBackendSearch } from "../backendClient.js";
import type { SearchDocsInput, SearchMode } from "../types.js";
import { AuthError } from "../types/auth.js";
import { DeviceAuthManager } from "../auth/DeviceAuthManager.js";
import { config } from "../config.js";

// Singleton DeviceAuthManager instance
let authManager: DeviceAuthManager | null = null;

/**
 * Get or create the DeviceAuthManager instance.
 */
function getAuthManager(): DeviceAuthManager {
  if (!authManager) {
    authManager = new DeviceAuthManager(config.backendUrl);
  }
  return authManager;
}

/**
 * Validates and normalizes the search input.
 * Applies defaults and validates constraints matching backend bean validation.
 */
function validateInput(input: any): SearchDocsInput {
  // Validate query
  if (!input.query || typeof input.query !== "string") {
    throw new Error("Invalid input: 'query' is required and must be a non-empty string");
  }

  const query = input.query.trim();
  if (query.length === 0) {
    throw new Error("Invalid input: 'query' cannot be empty or whitespace");
  }

  // Validate and default mode
  let mode: SearchMode | undefined = input.mode;
  if (mode !== undefined) {
    if (mode !== "traditional" && mode !== "semantic") {
      throw new Error(`Invalid input: 'mode' must be "traditional" or "semantic", got "${mode}"`);
    }
  } else {
    mode = "semantic"; // Default to semantic for MCP tool
  }

  // Validate and default page
  let page: number | undefined = input.page;
  if (page !== undefined) {
    if (!Number.isInteger(page) || page < 0) {
      throw new Error(`Invalid input: 'page' must be a non-negative integer, got ${page}`);
    }
  } else {
    page = 0; // Default page
  }

  // Validate and default pageSize
  let pageSize: number | undefined = input.pageSize;
  if (pageSize !== undefined) {
    if (!Number.isInteger(pageSize) || pageSize < 1 || pageSize > 100) {
      throw new Error(
        `Invalid input: 'pageSize' must be an integer between 1 and 100, got ${pageSize}`
      );
    }
  } else {
    pageSize = 10; // Default pageSize
  }

  // Validate traceId (optional, no validation if present)
  const traceId = input.traceId;

  return {
    query,
    mode,
    page,
    pageSize,
    traceId,
  };
}

/**
 * MCP tool handler for search_docs.
 */
export async function handleSearchDocs(args: any): Promise<any> {
  try {
    const validatedInput = validateInput(args);

    // Ensure authentication before making request
    console.error("[AUTH] Starting authentication check...");
    const authManager = getAuthManager();
    let accessToken: string;
    try {
      // Use non-blocking mode - return immediately if auth needed
      accessToken = await authManager.ensureAuthenticated(true);
      console.error(`[AUTH] Authentication successful, token length: ${accessToken.length}`);
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
          // Ensure it's not a generic error by making it clearly identifiable
          (error as any).code = "AUTHENTICATION_REQUIRED";
          throw error;
        }
        
        if (!authError.shouldRetry) {
          // Non-retryable auth error (e.g., timeout, invalid grant)
          throw new Error(
            `❌ Authentication failed: ${authError.message}. Please try again.`
          );
        }
      }
      // Retryable error - re-throw to let caller handle
      throw new Error(
        `⚠️ Authentication error: ${authError.message}. Please try again.`
      );
    }

    // Make authenticated search request
    let response;
    try {
      response = await callBackendSearch(validatedInput, accessToken);
    } catch (error: any) {
      // Handle 401/403 from backend
      if (error instanceof AuthError) {
        if (error.code === "unauthorized" && error.shouldRetry) {
          // Token invalid - clear and re-authenticate
          await authManager.logout();
          try {
            accessToken = await authManager.ensureAuthenticated();
            // Retry request with new token
            response = await callBackendSearch(validatedInput, accessToken);
          } catch (retryError: any) {
            throw new Error(
              `Re-authentication failed: ${retryError.message}. Please try again.`
            );
          }
        } else {
          // Other auth error (e.g., forbidden)
          throw new Error(`Authentication error: ${error.message}`);
        }
      } else {
        // Re-throw other errors
        throw error;
      }
    }

    return response;
  } catch (error: any) {
    // Re-throw validation errors as-is
    if (error.message.startsWith("Invalid input:")) {
      throw error;
    }

    // Re-throw auth errors as-is (already formatted)
    if (error.message.includes("Authentication") || error.message.includes("auth")) {
      throw error;
    }

    // Wrap other backend/client errors
    throw new Error(`Backend error: ${error.message}`);
  }
}