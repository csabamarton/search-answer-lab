/**
 * Type definitions for MCP server that match the Spring Boot backend DTOs.
 */

export type SearchMode = "traditional" | "semantic";

/**
 * Input for the search_docs tool (mirrors SearchRequest DTO).
 */
export interface SearchDocsInput {
  query: string;
  mode?: SearchMode;
  page?: number;
  pageSize?: number;
  traceId?: string;
}

/**
 * A single search result item (mirrors SearchResult DTO).
 */
export interface SearchResult {
  id: number;
  title: string;
  content: string;
  source: string;
  score: number;
  snippet: string;
}

/**
 * Search metadata (mirrors SearchMetadata DTO).
 */
export interface SearchMetadata {
  durationMs: number;
  totalResults: number;
  page: number;
  pageSize: number;
  searchMode: string;
  fallbackUsed: boolean;
}

/**
 * Complete search response (mirrors SearchResponse DTO).
 */
export interface SearchDocsResponse {
  results: SearchResult[];
  metadata: SearchMetadata;
  requestId: string;
}