/**
 * ResultsList component - Displays list of search results
 */

import ResultCard from './ResultCard'
import EmptyState from './EmptyState'
import LoadingState from './LoadingState'
import type { SearchResult } from '../../types'

interface ResultsListProps {
  results: SearchResult[]
  loading: boolean
  error: string | null
}

function ResultsList({ results, loading, error }: ResultsListProps) {
  if (loading) {
    return <LoadingState />
  }

  if (error) {
    return (
      <div className="text-center py-8 text-red-600">
        <p>Error: {error}</p>
      </div>
    )
  }

  if (!results || results.length === 0) {
    return <EmptyState />
  }

  return (
    <div className="space-y-4">
      {results.map((result, index) => (
        <ResultCard key={result.id || index} result={result} />
      ))}
    </div>
  )
}

export default ResultsList
