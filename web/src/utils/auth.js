import { unwrapData } from './apiPayload'

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_KEY = 'auth_user'

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY)
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY)
export const getUser = () => {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export const setAuth = (tokenResponse) => {
  if (!tokenResponse) return
  const t = unwrapData(tokenResponse) ?? tokenResponse
  if (t.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, t.accessToken)
  }
  if (t.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, t.refreshToken)
  }
  if (t.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(t.user))
  }
}

export const clearAuth = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
