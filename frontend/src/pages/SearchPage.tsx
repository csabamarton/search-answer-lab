/**
 * SearchPage - Main search interface page
 * Combines search input, mode toggle, results display, and metrics
 */

import { useState } from 'react'
import SearchInput from '../components/search/SearchInput'
import ModeToggle from '../components/search/ModeToggle'
import ResultsList from '../components/results/ResultsList'
import MetricsBar from '../components/metrics/MetricsBar'
import useSearch from '../hooks/useSearch'
import type { SearchMode } from '../types'

function SearchPage() {
  const [query, setQuery] = useState<string>('')
  const [mode, setMode] = useState<SearchMode>('traditional')

  const { results, metadata, loading, error, executeSearch } = useSearch()

  const handleSearch = () => {
    if (query.trim()) {
      executeSearch(query, mode)
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Search Header */}
      <div className="text-center mb-8">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Search the Knowledge Base
        </h2>
        <p className="text-gray-600">
          Compare traditional keyword search with AI-powered search
        </p>
      </div>

      {/* Search Controls */}
      <div className="mb-6 space-y-4">
        <SearchInput
          value={query}
          onChange={setQuery}
          onSubmit={handleSearch}
        />

        <div className="flex justify-center">
          <ModeToggle mode={mode} onChange={setMode} />
        </div>
      </div>

      {/* Metrics */}
      {metadata && <MetricsBar metadata={metadata} />}

      {/* Results */}
      <ResultsList results={results} loading={loading} error={error} />
    </div>
  )
}

export default SearchPage
