/**
 * HTTP client for communicating with Spring Boot backend.
 */
import type { SearchDocsInput, SearchDocsResponse } from "./types.js";
/**
 * Makes a POST request to the backend search endpoint.
 * Throws an error for non-2xx responses with status and response body.
 */
export declare function search(input: SearchDocsInput): Promise<SearchDocsResponse>;
//# sourceMappingURL=backendClient.d.ts.map