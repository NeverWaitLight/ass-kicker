import { clearAuth, getAccessToken, getRefreshToken, setAuth } from './auth'
import { syncAuth } from '../stores/auth'

const refreshAccessToken = async () => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  })
  if (!response.ok) {
    return false
  }
  const data = await response.json()
  setAuth(data)
  syncAuth()
  return true
}

export const apiFetch = async (url, options = {}) => {
  const headers = new Headers(options.headers || {})
  const token = getAccessToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(url, { ...options, headers })
  if (response.status !== 401) {
    return response
  }
  const refreshed = await refreshAccessToken()
  if (!refreshed) {
    clearAuth()
    syncAuth()
    window.location.href = '/login'
    return response
  }
  const retryHeaders = new Headers(options.headers || {})
  const newToken = getAccessToken()
  if (newToken) {
    retryHeaders.set('Authorization', `Bearer ${newToken}`)
  }
  if (options.body && !retryHeaders.has('Content-Type')) {
    retryHeaders.set('Content-Type', 'application/json')
  }
  return fetch(url, { ...options, headers: retryHeaders })
}
