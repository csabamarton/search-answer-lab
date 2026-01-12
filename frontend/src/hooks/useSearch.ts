/**
 * useSearch hook - Manages search state and execution
 */

import { useState, useCallback } from 'react'
import { executeSearch } from '../services/searchService'
import type { SearchResult, SearchMetadata, SearchMode } from '../types'

interface SearchOptions {
  page?: number
  pageSize?: number
}

interface UseSearchReturn {
  results: SearchResult[]
  metadata: SearchMetadata | null
  loading: boolean
  error: string | null
  executeSearch: (query: string, mode: SearchMode, options?: SearchOptions) => Promise<void>
  reset: () => void
}

/**
 * Custom hook for managing search state and execution
 * @returns Search state and functions
 */
function useSearch(): UseSearchReturn {
  const [results, setResults] = useState<SearchResult[]>([])
  const [metadata, setMetadata] = useState<SearchMetadata | null>(null)
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  const executeSearchQuery = useCallback(async (
    query: string,
    mode: SearchMode,
    options: SearchOptions = {}
  ) => {
    setLoading(true)
    setError(null)

    try {
      const response = await executeSearch(query, mode, options)

      setResults(response.results || [])
      setMetadata(response.metadata || null)
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Search failed'
      setError(errorMessage)
      setResults([])
      setMetadata(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const reset = useCallback(() => {
    setResults([])
    setMetadata(null)
    setError(null)
  }, [])

  return {
    results,
    metadata,
    loading,
    error,
    executeSearch: executeSearchQuery,
    reset,
  }
}

export default useSearch
