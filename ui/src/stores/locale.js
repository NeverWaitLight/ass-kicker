import { ref } from 'vue'
import i18n from '../i18n'

const LOCALE_KEY = 'app_locale'
const LOCALES = ['zh-CN', 'en-US']

const normalizeLocale = (value) => (LOCALES.includes(value) ? value : 'zh-CN')

export const currentLocale = ref('zh-CN')

export const initLocale = () => {
  if (typeof window === 'undefined') return
  const stored = localStorage.getItem(LOCALE_KEY)
  const locale = normalizeLocale(stored)
  currentLocale.value = locale
  i18n.global.locale.value = locale
}

export const setLocale = (value) => {
  const locale = normalizeLocale(value)
  currentLocale.value = locale
  i18n.global.locale.value = locale
  if (typeof window !== 'undefined') {
    localStorage.setItem(LOCALE_KEY, locale)
  }
}
