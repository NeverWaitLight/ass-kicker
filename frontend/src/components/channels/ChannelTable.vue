<template>
  <a-table
    :data-source="rows"
    :columns="columns"
    :pagination="pagination"
    :loading="loading"
    row-key="id"
    @change="handleChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'type'">
        <a-tag :color="getTypeColor(record.type)">{{ getTypeLabel(record.type) }}</a-tag>
      </template>
      <template v-else-if="column.key === 'createdAt'">
        {{ formatTimestamp(record.createdAt) }}
      </template>
      <template v-else-if="column.key === 'updatedAt'">
        {{ formatTimestamp(record.updatedAt) }}
      </template>
      <template v-else-if="column.key === 'actions'">
        <a-space>
          <a-button size="small" :disabled="!canTest" @click="$emit('test', record)">测试</a-button>
          <a-button size="small" :disabled="!canEdit" @click="$emit('edit', record)">编辑</a-button>
          <a-button size="small" danger :disabled="!canDelete" @click="$emit('delete', record)">删除</a-button>
        </a-space>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { formatTimestamp } from '../../utils/time'
import { useI18n } from 'vue-i18n'
import { getChannelTypeLabel } from '../../constants/channelTypes'

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pagination: { type: Object, default: () => ({}) },
  canTest: { type: Boolean, default: false },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false }
})

const emit = defineEmits(['test', 'edit', 'delete', 'page-change'])
const { t, te } = useI18n()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'actions', width: 220 }
]

const handleChange = (pager) => {
  if (!pager) return
  emit('page-change', pager)
}

const getTypeColor = (type) => {
  const colorMap = {
    'SMS': 'blue',
    'EMAIL': 'green',
    'IM': 'orange',
    'PUSH': 'purple'
  }
  return colorMap[type] || 'default'
}

const getTypeLabel = (type) => getChannelTypeLabel(type, t, te)
</script>
