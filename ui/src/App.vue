<template>
  <a-config-provider :theme="themeConfig" :locale="antdLocale">
    <router-view v-if="isPublic" />
    <a-layout v-else class="app-shell">
      <a-layout-header class="app-top-header">
        <div class="header-brand">
          <div class="brand-row">
            <img class="brand-logo" :src="logoUrl" :alt="t('brand.title')" />
            <span class="brand-title">{{ t('brand.title') }}</span>
          </div>
        </div>
        <a-space class="header-actions" size="middle">
          <ThemeToggle />
          <LocaleSwitcher />
          <UserMenu
            @profile="openProfileModal"
            @password="openPasswordModal"
            @apikeys="openApiKeysModal"
            @logout="logout"
          />
        </a-space>
      </a-layout-header>
      <a-layout class="app-body">
        <a-layout-sider
          v-model:collapsed="collapsed"
          collapsible
          :theme="siderTheme"
          width="max-content"
          class="app-sider"
          breakpoint="lg"
        >
          <a-menu
            mode="inline"
            :theme="siderTheme"
            :selectedKeys="selectedKeys"
            class="app-sider-menu"
            @click="onMenu"
          >
            <a-menu-item key="/send-records">
              <template #icon><HistoryOutlined /></template>
              {{ t('nav.sendRecords') }}
            </a-menu-item>
            <a-menu-item key="/templates">
              <template #icon><FileTextOutlined /></template>
              {{ t('nav.templates') }}
            </a-menu-item>
            <a-menu-item v-if="isAdmin" key="/global-variables">
              <template #icon><DatabaseOutlined /></template>
              {{ t('nav.globalVariables') }}
            </a-menu-item>
            <a-menu-item v-if="canViewChannel" key="/channels">
              <template #icon><ApiOutlined /></template>
              {{ t('nav.channels') }}
            </a-menu-item>
            <a-menu-item v-if="isAdmin" key="/users">
              <template #icon><TeamOutlined /></template>
              {{ t('nav.users') }}
            </a-menu-item>
          </a-menu>
        </a-layout-sider>
        <a-layout class="app-main">
          <a-layout-content class="app-content">
            <router-view />
          </a-layout-content>
        </a-layout>
      </a-layout>
    </a-layout>
    <UserAccountModals v-if="!isPublic" ref="accountModalsRef" />
  </a-config-provider>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message, theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth } from './utils/auth'
import { currentUser, syncAuth } from './stores/auth'
import { currentLocale } from './stores/locale'
import { currentTheme } from './stores/theme'
import { CHANNEL_PERMISSIONS, hasPermission } from './utils/permissions'
import {
  ApiOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  HistoryOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import logoUrl from './assets/logo.png'
import ThemeToggle from './components/ThemeToggle.vue'
import LocaleSwitcher from './components/LocaleSwitcher.vue'
import UserMenu from './components/UserMenu.vue'
import UserAccountModals from './components/UserAccountModals.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const accountModalsRef = ref(null)

const isPublic = computed(() => route.meta?.public)
const selectedKeys = computed(() => {
  const path = route.path
  if (path === '/') {
    return []
  }
  const base = `/${path.split('/')[1] || ''}`
  const menuKeys = ['/users', '/channels', '/templates', '/global-variables', '/send-records']
  if (menuKeys.includes(base)) {
    return [base]
  }
  if (path.startsWith('/templates/')) {
    return ['/templates']
  }
  if (path.startsWith('/channels/')) {
    return ['/channels']
  }
  if (path.startsWith('/send-records/')) {
    return ['/send-records']
  }
  return []
})
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const canViewChannel = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.view))
const siderTheme = computed(() => (currentTheme.value === 'dark' ? 'dark' : 'light'))
const themeConfig = computed(() => ({
  algorithm: currentTheme.value === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm
}))
const antdLocale = computed(() => (currentLocale.value === 'zh-CN' ? zhCN : enUS))

const onMenu = ({ key }) => {
  router.push(key)
}

const logout = () => {
  clearAuth()
  syncAuth()
  router.push('/login')
}

const openProfileModal = () => {
  accountModalsRef.value?.openProfile()
}

const openPasswordModal = () => {
  accountModalsRef.value?.openPassword()
}

const openApiKeysModal = () => {
  accountModalsRef.value?.openApiKeys()
}

watch(
  () => route.query.denied,
  (value) => {
    if (value) {
      message.warning(t('app.deniedChannel'))
      router.replace({ path: route.path, query: {} })
    }
  }
)
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-top-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  height: 56px;
  line-height: 56px;
  padding: 0 24px;
  z-index: 10;
}

.header-brand {
  display: flex;
  align-items: center;
  min-width: 0;
}

.brand-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  min-width: 0;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-actions {
  flex-shrink: 0;
  align-items: center;
}

.app-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  overflow: hidden;
}

.app-sider {
  flex-shrink: 0;
  align-self: stretch;
  overflow: auto;
}

.app-sider-menu {
  border-inline-end: none !important;
  width: max-content;
  min-width: min-content;
}

.app-sider-menu:not(.ant-menu-inline-collapsed) :deep(.ant-menu-item) {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start;
  gap: 8px;
}

.app-sider-menu:not(.ant-menu-inline-collapsed) :deep(.ant-menu-item .ant-menu-item-icon) {
  margin-inline-end: 0 !important;
  flex: 0 0 22px;
  width: 22px;
  min-width: 22px;
  height: 22px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.app-sider-menu:not(.ant-menu-inline-collapsed) :deep(.ant-menu-item .ant-menu-item-icon .anticon) {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: 0;
}

.app-sider-menu:not(.ant-menu-inline-collapsed) :deep(.ant-menu-title-content) {
  flex: 0 1 auto;
  overflow: visible !important;
  text-overflow: clip !important;
  white-space: nowrap !important;
  text-align: start;
}

.app-sider-menu.ant-menu-inline-collapsed :deep(.ant-menu-item) {
  justify-content: center;
  gap: 0;
  padding-inline: 16px !important;
}

.app-sider-menu.ant-menu-inline-collapsed :deep(.ant-menu-title-content) {
  display: none !important;
  flex: 0 0 0 !important;
  width: 0 !important;
  min-width: 0 !important;
  max-width: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  overflow: hidden !important;
  opacity: 0 !important;
  pointer-events: none;
}

.app-sider-menu.ant-menu-inline-collapsed :deep(.ant-menu-item .ant-menu-item-icon) {
  margin-inline: 0 !important;
  line-height: 1;
}

.app-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-content {
  flex: 1;
  overflow: auto;
  padding: 24px 32px 32px;
}

@media (max-width: 768px) {
  .app-top-header {
    padding: 0 16px;
    flex-wrap: wrap;
    height: auto;
    min-height: 56px;
    line-height: normal;
    padding-top: 8px;
    padding-bottom: 8px;
    gap: 8px;
  }

  .header-brand {
    flex: 1 1 auto;
    min-width: 0;
  }

  .header-actions {
    flex: 1 1 100%;
    justify-content: flex-end;
  }

  .app-content {
    padding: 16px 20px 24px;
  }
}
</style>
