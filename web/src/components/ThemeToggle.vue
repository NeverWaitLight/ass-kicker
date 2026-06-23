<template>
  <div class="theme-toggle">
    <a-switch v-model:checked="isDark" :checked-children="t('theme.dark')" :un-checked-children="t('theme.light')" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { currentTheme, setTheme } from '../stores/theme'

const { t } = useI18n()

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
