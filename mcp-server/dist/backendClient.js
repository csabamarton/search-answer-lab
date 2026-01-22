/**
 * HTTP client for communicating with Spring Boot backend.
 */
import { config } from "./config.js";
import { AuthError } from "./types/auth.js";
/**
 * Makes a POST request to the backend search endpoint.
 * Throws an error for non-2xx responses with status and response body.
 * @param input Search input parameters
 * @param accessToken Optional access token for authentication
 */
export async function search(input, accessToken) {
    const url = `${config.backendUrl}/api/search`;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), config.requestTimeoutMs);
    try {
        const headers = {
            "Content-Type": "application/json",
        };
        // Add Authorization header if token provided
        if (accessToken) {
            headers["Authorization"] = `Bearer ${accessToken}`;
        }
        const response = await fetch(url, {
            method: "POST",
            headers,
            body: JSON.stringify({
                query: input.query,
                mode: input.mode,
                page: input.page,
                pageSize: input.pageSize,
                traceId: input.traceId,
            }),
            signal: controller.signal,
        });
        clearTimeout(timeoutId);
        if (!response.ok) {
            let errorBody;
            try {
                errorBody = await response.text();
            }
            catch {
                errorBody = "Unable to read response body";
            }
            // Handle 401 Unauthorized - token invalid or expired
            if (response.status === 401) {
                throw new AuthError("Authentication required or token expired", "unauthorized", true // Should retry with re-authentication
                );
            }
            // Handle 403 Forbidden - missing scope
            if (response.status === 403) {
                throw new AuthError("Insufficient permissions. Required scope: docs:search", "forbidden", false);
            }
            const error = new Error(`Backend error: ${response.status} ${response.statusText}. ${errorBody}`);
            error.status = response.status;
            throw error;
        }
        return (await response.json());
    }
    catch (error) {
        clearTimeout(timeoutId);
        if (error.name === "AbortError") {
            throw new Error(`Request timeout after ${config.requestTimeoutMs}ms`);
        }
        // Re-throw AuthError as-is
        if (error instanceof AuthError) {
            throw error;
        }
        if (error.status) {
            throw error; // Already formatted backend error
        }
        throw new Error(`Network error: ${error.message}`);
    }
}
//# sourceMappingURL=backendClient.js.map