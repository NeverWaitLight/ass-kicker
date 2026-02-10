import { ref } from 'vue'

const THEME_KEY = 'app_theme'
const THEMES = ['light', 'dark']

const normalizeTheme = (value) => (THEMES.includes(value) ? value : 'light')

const applyTheme = (theme) => {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = theme
}

export const currentTheme = ref('light')

export const initTheme = () => {
  if (typeof window === 'undefined') return
  const stored = localStorage.getItem(THEME_KEY)
  const prefersDark =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  const theme = normalizeTheme(stored || (prefersDark ? 'dark' : 'light'))
  currentTheme.value = theme
  applyTheme(theme)
}

export const setTheme = (value) => {
  const theme = normalizeTheme(value)
  currentTheme.value = theme
  if (typeof window !== 'undefined') {
    localStorage.setItem(THEME_KEY, theme)
    applyTheme(theme)
  }
}

export const toggleTheme = () => {
  setTheme(currentTheme.value === 'dark' ? 'light' : 'dark')
}
