<template>
  <div class="theme-toggle">
    <a-switch v-model:checked="isDark" checked-children="暗" un-checked-children="亮" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { currentTheme, setTheme } from '../stores/theme'

const isDark = computed({
  get: () => currentTheme.value === 'dark',
  set: (value) => setTheme(value ? 'dark' : 'light')
})
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
