/**
 * Authentication service - OAuth Device Code Flow API calls
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface AuthorizeResponse {
  status: string
  message: string
}

interface ErrorResponse {
  error: string
  error_description: string
}

/**
 * Authorize a device code with user credentials
 */
export async function authorizeDeviceCode(
  userCode: string,
  username: string,
  password: string
): Promise<AuthorizeResponse> {
  const response = await fetch(`${API_BASE_URL}/oauth/device/authorize`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      user_code: userCode,
      username,
      password,
    }),
  })

  if (!response.ok) {
    const errorData = (await response.json().catch(() => ({
      error: 'Authorization failed',
      error_description: `HTTP ${response.status}`,
    }))) as ErrorResponse

    throw new Error(errorData.error_description || errorData.error || 'Authorization failed')
  }

  return (await response.json()) as AuthorizeResponse
}
