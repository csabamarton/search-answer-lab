/**
 * Application-wide constants
 */

import type { SearchMode } from '../types'

export const SEARCH_MODES: Record<string, SearchMode> = {
  TRADITIONAL: 'traditional',
  SEMANTIC: 'semantic',
} as const

export const DEFAULT_PAGE_SIZE = 10

export const API_ENDPOINTS = {
  SEARCH: '/api/search',
  SUGGESTIONS: '/api/search/suggestions',
  HEALTH: '/api/health',
} as const

export const STATUS_CODES = {
  SUCCESS: 'success',
  ERROR: 'error',
  LOADING: 'loading',
} as const

export type StatusCode = typeof STATUS_CODES[keyof typeof STATUS_CODES]
