<template>
  <a-modal
    :open="open"
    title="确认删除"
    ok-text="删除"
    ok-type="danger"
    cancel-text="取消"
    :confirm-loading="loading"
    @ok="$emit('confirm')"
    @cancel="$emit('cancel')"
  >
    <p>确认删除渠道 <strong>{{ channel?.name || '-' }}</strong> 吗？此操作不可撤销。</p>
  </a-modal>
</template>

<script setup>
defineProps({
  open: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  channel: { type: Object, default: null }
})

defineEmits(['confirm', 'cancel'])
</script>
