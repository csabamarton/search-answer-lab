/**
 * SearchOptions component - Additional search configuration options
 * Allows users to configure search behavior (future: filters, sorting)
 */

interface SearchOptionsProps {
  options?: Record<string, unknown>
  onChange?: (options: Record<string, unknown>) => void
}

function SearchOptions({ options, onChange }: SearchOptionsProps) {
  return (
    <div className="flex gap-4 items-center text-sm">
      <div className="text-gray-600">
        {/* TODO: Add search options like filters, sorting, pagination */}
        <span>Search options will be available here</span>
      </div>
    </div>
  )
}

export default SearchOptions
