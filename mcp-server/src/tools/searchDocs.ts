/**
 * MCP tool handler for search_docs.
 * Validates input and forwards to backend client.
 */

import { search as callBackendSearch } from "../backendClient.js";
import type { SearchDocsInput, SearchMode } from "../types.js";

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
    const response = await callBackendSearch(validatedInput);
    return response;
  } catch (error: any) {
    // Re-throw validation errors as-is
    if (error.message.startsWith("Invalid input:")) {
      throw error;
    }

    // Wrap backend/client errors
    throw new Error(`Backend error: ${error.message}`);
  }
}