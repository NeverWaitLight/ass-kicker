<template>
  <a-modal
    :open="open"
    title="确认删除"
    ok-type="danger"
    :confirm-loading="loading"
    @cancel="$emit('cancel')"
  >
    <template #footer>
      <a-space>
        <a-button title="撤销" @click="$emit('cancel')">
          <template #icon><UndoOutlined /></template>
        </a-button>
        <a-button type="primary" danger :loading="loading" title="删除" @click="$emit('confirm')">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </a-space>
    </template>
    <p>确认删除渠道 <strong>{{ channel?.name || '-' }}</strong> 吗？此操作不可撤销。</p>
  </a-modal>
</template>

<script setup>
import { DeleteOutlined, UndoOutlined } from '@ant-design/icons-vue'

defineProps({
  open: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  channel: { type: Object, default: null }
})

defineEmits(['confirm', 'cancel'])
</script>
