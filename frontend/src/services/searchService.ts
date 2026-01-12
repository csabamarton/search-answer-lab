/**
 * Search service - Handles search API requests
 */

import { api } from './api'
import { generateRequestId } from '../utils/formatters'
import type { SearchRequest, SearchResponse, SearchMode } from '../types'

interface SearchOptions {
  page?: number
  pageSize?: number
}

/**
 * Execute a search query
 * @param query - Search query string
 * @param mode - Search mode ('traditional' or 'semantic')
 * @param options - Additional search options
 */
export async function executeSearch(
  query: string,
  mode: SearchMode = 'traditional',
  options: SearchOptions = {}
): Promise<SearchResponse> {
  const searchRequest: SearchRequest = {
    query,
    mode,
    page: options.page || 0,
    pageSize: options.pageSize || 10,
    traceId: generateRequestId(),
  }

  return await api.post<SearchResponse>('/api/search', searchRequest)
}

/**
 * Get search suggestions
 * @param query - Partial query string
 */
export async function getSuggestions(query: string): Promise<string[]> {
  return await api.get<string[]>(`/api/search/suggestions?q=${encodeURIComponent(query)}`)
}

interface HealthResponse {
  status: string
  timestamp: string
}

/**
 * Check API health
 */
export async function checkHealth(): Promise<HealthResponse> {
  return await api.get<HealthResponse>('/api/health')
}
