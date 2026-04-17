<template>
  <a-select
    class="locale-switcher"
    size="small"
    :value="currentLocale"
    :options="options"
    :disabled="options.length === 1"
    :dropdown-match-select-width="false"
    @update:value="setLocale"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { currentLocale, setLocale } from '../stores/locale'

const { t } = useI18n()

const options = computed(() => [
  { label: t('locale.zhCN'), value: 'zh-CN' },
  { label: t('locale.enUS'), value: 'en-US' }
])
</script>

<style scoped>
.locale-switcher {
  min-width: 112px;
}
</style>
