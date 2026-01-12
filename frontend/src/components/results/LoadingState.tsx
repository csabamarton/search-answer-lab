/**
 * LoadingState component - Shown while search is in progress
 */
function LoadingState() {
  return (
    <div className="text-center py-12">
      <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
      <p className="text-gray-600">Searching...</p>
    </div>
  )
}

export default LoadingState
