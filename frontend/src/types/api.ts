/**
 * TypeScript type definitions for API-related types
 */

export interface ApiError {
  message: string
  status?: number
  code?: string
}

export interface ApiResponse<T> {
  data: T
  status: number
}

export interface FetchOptions extends RequestInit {
  headers?: Record<string, string>
}
