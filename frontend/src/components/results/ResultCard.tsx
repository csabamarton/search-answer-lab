/**
 * ResultCard component - Individual search result card
 * Displays document title, snippet, and metadata
 */

import type { SearchResult } from '../../types'

interface ResultCardProps {
  result: SearchResult
}

function ResultCard({ result }: ResultCardProps) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-2">
        <h3 className="text-lg font-semibold text-gray-900">
          {result.title}
        </h3>
        {result.score && (
          <span className="text-xs text-gray-500 ml-4">
            Score: {result.score.toFixed(2)}
          </span>
        )}
      </div>

      <p className="text-gray-700 mb-3 line-clamp-3">
        {result.snippet || result.content}
      </p>

      <div className="flex items-center text-sm text-gray-500">
        <span className="px-2 py-1 bg-gray-100 rounded text-xs">
          {result.source}
        </span>
      </div>
    </div>
  )
}

export default ResultCard
