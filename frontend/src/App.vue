<template>
  <router-view v-if="isPublic" />
  <a-layout v-else class="app-shell">
    <a-layout-header class="app-header">
      <div class="brand">
        <div class="brand-mark">
          <div class="brand-row">
            <img class="brand-logo" :src="logoUrl" alt="Ass Kicker logo" />
            <div class="brand-title">Ass Kicker</div>
          </div>
        </div>
      </div>
      <a-menu mode="horizontal" :theme="menuTheme" :selectedKeys="[selectedKey]" @click="onMenu">
        <a-menu-item key="/">首页</a-menu-item>
        <a-menu-item v-if="isAdmin" key="/users">用户管理</a-menu-item>
        <a-menu-item v-if="canViewChannel" key="/channels">通道管理</a-menu-item>
      </a-menu>
      <a-space class="header-actions" size="middle">
        <ThemeToggle />
        <UserMenu @settings="goSettings" @logout="logout" />
      </a-space>
    </a-layout-header>
    <a-layout-content class="app-content">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth } from './utils/auth'
import { currentUser, syncAuth } from './stores/auth'
import { currentTheme } from './stores/theme'
import { CHANNEL_PERMISSIONS, hasPermission } from './utils/permissions'
import logoUrl from './assets/logo.png'
import ThemeToggle from './components/ThemeToggle.vue'
import UserMenu from './components/UserMenu.vue'

const route = useRoute()
const router = useRouter()

const isPublic = computed(() => route.meta?.public)
const selectedKey = computed(() => {
  const path = route.path
  if (path === '/') {
    return '/'
  }
  const base = `/${path.split('/')[1] || ''}`
  const menuKeys = ['/', '/users', '/channels', '/settings']
  if (menuKeys.includes(base)) {
    return base
  }
  return path
})
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const canViewChannel = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.view))
const menuTheme = computed(() => (currentTheme.value === 'dark' ? 'dark' : 'light'))

const onMenu = ({ key }) => {
  router.push(key)
}

const logout = () => {
  clearAuth()
  syncAuth()
  router.push('/login')
}

const goSettings = () => {
  router.push('/settings')
}

watch(
  () => route.query.denied,
  (value) => {
    if (value) {
      message.warning('当前账号暂无权限访问该页面')
      router.replace({ path: route.path, query: {} })
    }
  }
)
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
  justify-content: flex-start;
  background: var(--header-bg);
  color: var(--header-text);
  border-bottom: 1px solid var(--header-border);
  padding: 0 32px;
}

.brand {
  display: flex;
  align-items: center;
}

.brand-mark {
  display: flex;
  align-items: center;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--brand-logo-bg);
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--header-text);
}

.header-actions {
  margin-left: auto;
  align-items: center;
}

.app-content {
  padding: 32px;
}
</style>
