import { createI18n } from 'vue-i18n'
import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'

export const DEFAULT_LOCALE = 'zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
