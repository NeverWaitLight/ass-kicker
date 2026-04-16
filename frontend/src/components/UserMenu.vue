<template>
  <a-dropdown :trigger="['click']">
    <a-button type="link" class="user-chip">
      <span class="user-name">{{ displayName }}</span>
    </a-button>
    <template #overlay>
      <a-menu :theme="menuTheme" @click="onMenuClick">
        <a-menu-item key="profile">{{ t('user.personalSettings') }}</a-menu-item>
        <a-menu-item key="password">{{ t('user.changePassword') }}</a-menu-item>
        <a-menu-item key="apikeys">{{ t('user.apiKeyManagement') }}</a-menu-item>
        <a-menu-divider />
        <a-menu-item key="logout">{{ t('user.logout') }}</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { currentUser } from '../stores/auth'
import { currentTheme } from '../stores/theme'

const { t } = useI18n()

const emit = defineEmits(['profile', 'password', 'apikeys', 'logout'])

const displayName = computed(() => currentUser.value?.username || t('user.fallbackName'))
const menuTheme = computed(() => (currentTheme.value === 'dark' ? 'dark' : 'light'))

const onMenuClick = ({ key }) => {
  if (key === 'profile') emit('profile')
  if (key === 'password') emit('password')
  if (key === 'apikeys') emit('apikeys')
  if (key === 'logout') emit('logout')
}
</script>

<style scoped>
.user-chip {
  padding: 0;
  border: none;
  box-shadow: none;
  background: transparent;
}

.user-name {
  font-weight: 500;
}
</style>
