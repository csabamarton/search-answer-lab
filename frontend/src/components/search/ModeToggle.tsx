/**
 * ModeToggle component - Toggle between Traditional and AI search modes
 */

import type { SearchMode } from '../../types'

interface ModeToggleProps {
  mode: SearchMode
  onChange: (mode: SearchMode) => void
}

function ModeToggle({ mode, onChange }: ModeToggleProps) {
  return (
    <div className="inline-flex rounded-lg border border-gray-300 bg-white p-1">
      <button
        onClick={() => onChange('traditional')}
        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
          mode === 'traditional'
            ? 'bg-blue-600 text-white'
            : 'text-gray-700 hover:bg-gray-100'
        }`}
      >
        Traditional Search
      </button>
      <button
        onClick={() => onChange('semantic')}
        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
          mode === 'semantic'
            ? 'bg-blue-600 text-white'
            : 'text-gray-700 hover:bg-gray-100'
        }`}
      >
        Semantic Search
      </button>
    </div>
  )
}

export default ModeToggle
