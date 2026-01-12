/**
 * MetricsBar component - Displays search performance metrics
 */

import MetricCard from './MetricCard'
import type { SearchMetadata } from '../../types'

interface MetricsBarProps {
  metadata: SearchMetadata | null
}

function MetricsBar({ metadata }: MetricsBarProps) {
  if (!metadata) return null

  const metrics = [
    {
      label: 'Results',
      value: metadata.totalResults || 0,
      unit: 'docs'
    },
    {
      label: 'Duration',
      value: metadata.durationMs || 0,
      unit: 'ms'
    },
    {
      label: 'Mode',
      value: metadata.searchMode || 'N/A',
      unit: ''
    }
  ]

  return (
    <div className="grid grid-cols-3 gap-4 mb-6">
      {metrics.map((metric, index) => (
        <MetricCard key={index} {...metric} />
      ))}
    </div>
  )
}

export default MetricsBar
