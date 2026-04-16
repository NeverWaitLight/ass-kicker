import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles.css'
import App from './App.vue'
import router from './router'
import { initTheme } from './stores/theme'
import { initLocale } from './stores/locale'
import i18n from './i18n'

const app = createApp(App)
app.use(Antd)
app.use(i18n)
app.use(router)
initTheme()
initLocale()
app.mount('#app')
