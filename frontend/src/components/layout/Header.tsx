/**
 * Header component - Top navigation bar
 */
function Header() {
  return (
    <header className="bg-white shadow-sm border-b">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800">
            Search Answer Lab
          </h1>
          <nav className="text-sm text-gray-600">
            <span>Traditional vs AI Search</span>
          </nav>
        </div>
      </div>
    </header>
  )
}

export default Header
