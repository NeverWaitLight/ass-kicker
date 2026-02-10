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
  if (tokenResponse.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, tokenResponse.accessToken)
  }
  if (tokenResponse.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)
  }
  if (tokenResponse.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(tokenResponse.user))
  }
}

export const clearAuth = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
