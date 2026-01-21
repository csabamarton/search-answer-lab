/**
 * AuthorizationPage - Device code authorization page
 * Allows users to authorize MCP access by entering device code and credentials
 */

import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { authorizeDeviceCode } from '../services/authService'

function AuthorizationPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [userCode, setUserCode] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [timeRemaining, setTimeRemaining] = useState(600) // 10 minutes in seconds

  // Pre-fill code from URL parameter
  useEffect(() => {
    const codeFromUrl = searchParams.get('code')
    if (codeFromUrl) {
      setUserCode(codeFromUrl.toUpperCase())
    }
  }, [searchParams])

  // Countdown timer
  useEffect(() => {
    const interval = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 0) {
          clearInterval(interval)
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [])

  // Auto-format device code input (XXXX-XXXX)
  const handleCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '')
    if (value.length > 4) {
      value = value.slice(0, 4) + '-' + value.slice(4, 8)
    }
    setUserCode(value)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      await authorizeDeviceCode(userCode, username, password)
      setSuccess(true)
    } catch (err: any) {
      setError(
        err.response?.data?.error_description ||
          err.message ||
          'Authorization failed. Please check your code and credentials.'
      )
    } finally {
      setLoading(false)
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-600 to-purple-900 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full bg-white rounded-xl shadow-2xl p-8">
        {/* Logo/Icon */}
        <div className="flex justify-center mb-6">
          <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center">
            <svg
              className="w-10 h-10 text-purple-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
              />
            </svg>
          </div>
        </div>

        <h1 className="text-2xl font-bold text-gray-900 text-center mb-2">
          Authorize MCP Access
        </h1>
        <p className="text-sm text-gray-600 text-center mb-8">
          Search Answer Lab requests access to your documents
        </p>

        {/* Success Message */}
        {success && (
          <div className="mb-6 p-4 bg-green-50 border-2 border-green-500 rounded-lg text-center">
            <div className="text-4xl mb-2">✅</div>
            <h2 className="text-lg font-semibold text-green-900 mb-1">
              Authorization Successful!
            </h2>
            <p className="text-sm text-green-700">
              You can now return to Claude Desktop and continue your search.
            </p>
          </div>
        )}

        {/* Error Message */}
        {error && !success && (
          <div className="mb-6 p-4 bg-red-50 border-2 border-red-500 rounded-lg">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {/* Authorization Form */}
        {!success && (
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Device Code */}
            <div>
              <label
                htmlFor="userCode"
                className="block text-sm font-semibold text-gray-700 mb-1"
              >
                Device Code
              </label>
              <input
                type="text"
                id="userCode"
                value={userCode}
                onChange={handleCodeChange}
                placeholder="XXXX-XXXX"
                maxLength={9}
                pattern="[A-Z0-9]{4}-[A-Z0-9]{4}"
                required
                autoFocus
                className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg text-center text-lg font-mono font-bold tracking-wider focus:outline-none focus:border-purple-500 focus:ring-2 focus:ring-purple-200"
              />
            </div>

            {/* Username */}
            <div>
              <label
                htmlFor="username"
                className="block text-sm font-semibold text-gray-700 mb-1"
              >
                Username
              </label>
              <input
                type="text"
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
                className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-purple-500 focus:ring-2 focus:ring-purple-200"
              />
            </div>

            {/* Password */}
            <div>
              <label
                htmlFor="password"
                className="block text-sm font-semibold text-gray-700 mb-1"
              >
                Password
              </label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-purple-500 focus:ring-2 focus:ring-purple-200"
              />
            </div>

            {/* Permissions */}
            <div className="bg-gray-50 rounded-lg p-4 border-l-4 border-purple-500">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">
                This will grant access to:
              </h3>
              <ul className="space-y-2">
                <li className="flex items-center text-sm text-gray-700">
                  <span className="text-green-500 font-bold mr-2">✓</span>
                  Search your technical documents
                </li>
                <li className="flex items-center text-sm text-gray-700">
                  <span className="text-green-500 font-bold mr-2">✓</span>
                  Read document content
                </li>
                <li className="flex items-center text-sm text-gray-700">
                  <span className="text-green-500 font-bold mr-2">✓</span>
                  View search results
                </li>
              </ul>
            </div>

            {/* Timer */}
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-center">
              <p className="text-sm text-red-800">
                Code expires in <strong>{formatTime(timeRemaining)}</strong>
              </p>
            </div>

            {/* Buttons */}
            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={() => navigate('/')}
                className="flex-1 px-4 py-3 bg-gray-200 text-gray-700 rounded-lg font-semibold hover:bg-gray-300 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 px-4 py-3 bg-purple-600 text-white rounded-lg font-semibold hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {loading ? 'Authorizing...' : 'Authorize'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default AuthorizationPage
