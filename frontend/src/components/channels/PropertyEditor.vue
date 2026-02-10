<template>
  <section class="property-editor">
    <a-table
      :data-source="rows"
      :columns="columns"
      :pagination="false"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'key'">
          <a-input
            :value="record.key"
            placeholder="属性键"
            :status="rowInvalidSet.has(record.id) ? 'error' : ''"
            @update:value="(val) => updateRow(record.id, { key: val })"
          />
        </template>
        <template v-else-if="column.key === 'type'">
          <a-select
            :value="record.type"
            style="width: 140px"
            :options="typeOptions"
            @update:value="(val) => changeType(record, val)"
          />
        </template>
        <template v-else-if="column.key === 'value'">
          <a-input
            v-if="record.type === 'string'"
            :value="record.value"
            placeholder="属性值"
            @update:value="(val) => updateRow(record.id, { value: val })"
          />
          <ObjectValueEditor
            v-else
            :rows="record.objectRows || []"
            :invalid-ids="objectInvalidMap[record.id] || new Set()"
            :error="objectErrorMap[record.id] || ''"
            @update:rows="(val) => updateRow(record.id, { objectRows: val })"
          />
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button size="small" danger @click="removeRow(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <div class="property-editor__footer">
      <a-button type="dashed" @click="addRow">添加属性</a-button>
      <span v-if="error" class="property-editor__error">{{ error }}</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import ObjectValueEditor from './ObjectValueEditor.vue'
import { createObjectRow, createPropertyRow } from '../../utils/propertyRows'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  rowInvalidIds: { type: [Array, Set], default: () => new Set() },
  objectInvalidIds: { type: Object, default: () => ({}) },
  objectErrors: { type: Object, default: () => ({}) },
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:rows'])

const rowInvalidSet = computed(() =>
  props.rowInvalidIds instanceof Set ? props.rowInvalidIds : new Set(props.rowInvalidIds)
)

const objectInvalidMap = computed(() => props.objectInvalidIds || {})
const objectErrorMap = computed(() => props.objectErrors || {})

const typeOptions = [
  { value: 'string', label: '字符串' },
  { value: 'object', label: '对象' }
]

const columns = [
  { title: '键', dataIndex: 'key', key: 'key', width: 200 },
  { title: '值类型', dataIndex: 'type', key: 'type', width: 160 },
  { title: '值', dataIndex: 'value', key: 'value' },
  { title: '操作', key: 'actions', width: 120 }
]

const updateRow = (id, patch) => {
  const next = props.rows.map((row) => (row.id === id ? { ...row, ...patch } : row))
  emit('update:rows', next)
}

const changeType = (record, type) => {
  if (!type) return
  if (type === 'object' && (!record.objectRows || record.objectRows.length === 0)) {
    updateRow(record.id, { type, objectRows: [createObjectRow()] })
    return
  }
  updateRow(record.id, { type })
}

const addRow = () => {
  emit('update:rows', [...props.rows, createPropertyRow()])
}

const removeRow = (id) => {
  emit('update:rows', props.rows.filter((row) => row.id !== id))
}
</script>

<style scoped>
.property-editor__footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.property-editor__error {
  color: #ff4d4f;
  font-size: 12px;
}
</style>
