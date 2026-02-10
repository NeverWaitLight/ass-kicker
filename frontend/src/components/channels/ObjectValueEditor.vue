<template>
  <section class="object-editor">
    <a-table
      :data-source="rows"
      :columns="columns"
      :pagination="false"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'key'">
          <a-input
            :value="record.key"
            placeholder="对象键"
            :status="invalidIdsSet.has(record.id) ? 'error' : ''"
            @update:value="(val) => updateRow(record.id, { key: val })"
          />
        </template>
        <template v-else-if="column.key === 'value'">
          <a-input
            :value="record.value"
            placeholder="对象值"
            @update:value="(val) => updateRow(record.id, { value: val })"
          />
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button size="small" danger @click="removeRow(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
    <div class="object-editor__footer">
      <a-button type="dashed" size="small" @click="addRow">添加对象字段</a-button>
      <span v-if="error" class="object-editor__error">{{ error }}</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { createObjectRow } from '../../utils/propertyRows'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  invalidIds: { type: [Array, Set], default: () => new Set() },
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:rows'])

const invalidIdsSet = computed(() =>
  props.invalidIds instanceof Set ? props.invalidIds : new Set(props.invalidIds)
)

const columns = [
  { title: '键', dataIndex: 'key', key: 'key', width: 160 },
  { title: '值', dataIndex: 'value', key: 'value' },
  { title: '操作', key: 'actions', width: 100 }
]

const updateRow = (id, patch) => {
  const next = props.rows.map((row) => (row.id === id ? { ...row, ...patch } : row))
  emit('update:rows', next)
}

const addRow = () => {
  emit('update:rows', [...props.rows, createObjectRow()])
}

const removeRow = (id) => {
  emit('update:rows', props.rows.filter((row) => row.id !== id))
}
</script>

<style scoped>
.object-editor__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.object-editor__error {
  color: #ff4d4f;
  font-size: 12px;
}
</style>
