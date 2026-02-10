import { beforeEach, describe, expect, it } from 'vitest'
import { currentTheme, initTheme, setTheme, toggleTheme } from '../theme'

const resetEnvironment = () => {
  localStorage.clear()
  document.documentElement.dataset.theme = ''
  currentTheme.value = 'light'
}

describe('theme store', () => {
  beforeEach(() => {
    resetEnvironment()
  })

  it('initializes from localStorage', () => {
    localStorage.setItem('app_theme', 'dark')
    initTheme()
    expect(currentTheme.value).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('toggles theme and persists selection', () => {
    setTheme('light')
    toggleTheme()
    expect(currentTheme.value).toBe('dark')
    expect(localStorage.getItem('app_theme')).toBe('dark')
  })
})
