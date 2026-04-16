<template>
  <a-table
    class="data-list-table data-list-table--fluid"
    :data-source="rows"
    :columns="columns"
    :pagination="pagination"
    :loading="loading"
    size="middle"
    bordered
    row-key="id"
    @change="handleChange"
  >
    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'ordinal'">
        {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
      </template>
      <template v-else-if="column.key === 'name'">
        <span class="data-list-cell-ellipsis" :title="record.name">{{ record.name }}</span>
      </template>
      <template v-else-if="column.key === 'description'">
        <span class="data-list-cell-ellipsis" :title="record.description">{{ record.description || '—' }}</span>
      </template>
      <template v-else-if="column.key === 'type'">
        <a-tag :color="getTypeColor(record.type)">{{ getTypeLabel(record.type) }}</a-tag>
      </template>
      <template v-else-if="column.key === 'recipientRules'">
        {{ recipientRulesLabel(record) }}
      </template>
      <template v-else-if="column.key === 'createdAt'">
        <span class="data-list-cell-time">{{ formatTimestamp(record.createdAt) }}</span>
      </template>
      <template v-else-if="column.key === 'updatedAt'">
        <span class="data-list-cell-time">{{ formatTimestamp(record.updatedAt) }}</span>
      </template>
      <template v-else-if="column.key === 'actions'">
        <a-space :size="4" wrap>
          <a-button type="link" size="small" class="data-list-action-link" :disabled="!canTest" @click="$emit('test', record)">测试</a-button>
          <a-button type="link" size="small" class="data-list-action-link" :disabled="!canEdit" @click="$emit('edit', record)">编辑</a-button>
          <a-button type="link" size="small" danger :disabled="!canDelete" @click="$emit('delete', record)">删除</a-button>
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

const recipientRulesLabel = (record) => {
  const inc = record.includeRecipientRegex?.trim?.()
  const exc = record.excludeRecipientRegex?.trim?.()
  if (inc || exc) return '已配置'
  return '—'
}

/** 不设 scroll.x，避免整表最小宽度撑出横向滚动条；未设宽度的列会吃掉剩余空间 */
const columns = [
  { title: '序号', key: 'ordinal', width: 56, align: 'center' },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '类型', dataIndex: 'type', key: 'type', width: 96, align: 'center' },
  { title: '收件人规则', key: 'recipientRules', width: 96, align: 'center' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 168 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 168 },
  { title: '操作', key: 'actions', width: 188, align: 'center' }
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
