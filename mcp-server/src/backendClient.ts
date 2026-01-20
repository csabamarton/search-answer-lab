/**
 * HTTP client for communicating with Spring Boot backend.
 */

import { config } from "./config.js";
import type { SearchDocsInput, SearchDocsResponse } from "./types.js";

/**
 * Makes a POST request to the backend search endpoint.
 * Throws an error for non-2xx responses with status and response body.
 */
export async function search(input: SearchDocsInput): Promise<SearchDocsResponse> {
  const url = `${config.backendUrl}/api/search`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), config.requestTimeoutMs);

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
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
      let errorBody: string;
      try {
        errorBody = await response.text();
      } catch {
        errorBody = "Unable to read response body";
      }

      const error = new Error(
        `Backend error: ${response.status} ${response.statusText}. ${errorBody}`
      );
      (error as any).status = response.status;
      throw error;
    }

    return (await response.json()) as SearchDocsResponse;
  } catch (error: any) {
    clearTimeout(timeoutId);

    if (error.name === "AbortError") {
      throw new Error(`Request timeout after ${config.requestTimeoutMs}ms`);
    }

    if (error.status) {
      throw error; // Already formatted backend error
    }

    throw new Error(`Network error: ${error.message}`);
  }
}