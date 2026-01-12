/**
 * TypeScript type definitions for search-related data structures
 */

export interface SearchRequest {
  query: string
  mode: SearchMode
  page?: number
  pageSize?: number
  traceId?: string
}

export interface SearchResponse {
  results: SearchResult[]
  metadata: SearchMetadata
  requestId: string
}

export interface SearchResult {
  id: number
  title: string
  content: string
  source: string
  score?: number
  snippet?: string
}

export interface SearchMetadata {
  durationMs: number
  totalResults: number
  page: number
  pageSize: number
  searchMode: SearchMode
  fallbackUsed: boolean
}

export type SearchMode = 'traditional' | 'semantic'

export interface SearchState {
  results: SearchResult[]
  metadata: SearchMetadata | null
  loading: boolean
  error: string | null
}
