<template>
  <a-dropdown :trigger="['click']">
    <a-button type="link" class="user-chip">
      <span class="user-name">{{ displayName }}</span>
    </a-button>
    <template #overlay>
      <a-menu :theme="menuTheme" @click="onMenuClick">
        <a-menu-item key="settings">用户设置</a-menu-item>
        <a-menu-item key="logout">退出</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { currentUser } from '../stores/auth'
import { currentTheme } from '../stores/theme'

const emit = defineEmits(['settings', 'logout'])

const displayName = computed(() => currentUser.value?.username || '用户')
const menuTheme = computed(() => (currentTheme.value === 'dark' ? 'dark' : 'light'))

const onMenuClick = ({ key }) => {
  if (key === 'settings') emit('settings')
  if (key === 'logout') emit('logout')
}
</script>

<style scoped>
.user-chip {
  padding: 0;
  color: var(--header-text);
  border: none;
  box-shadow: none;
  background: transparent;
}

.user-name {
  font-weight: 500;
}

</style>
