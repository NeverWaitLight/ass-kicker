<template>
  <section class="global-variable-layout">
    <div class="global-variable-header">
      <div>
        <h2>{{ t('globalVariable.title') }}</h2>
        <p>{{ t('globalVariable.subtitle') }}</p>
      </div>
      <div class="global-variable-actions">
        <a-input-search
          v-model:value="searchText"
          :placeholder="t('globalVariable.searchPlaceholder')"
          allow-clear
          enter-button
          style="width: 240px"
          @search="onSearch"
        >
          <template #enterButton>
            <SearchOutlined />
          </template>
        </a-input-search>
        <a-button class="global-variable-icon-btn" :title="t('common.reset')" @click="resetQuery">
          <template #icon><ReloadOutlined /></template>
        </a-button>
        <a-button type="primary" class="global-variable-add-btn" :title="t('globalVariable.createTitle')" @click="openCreate">
          <template #icon><PlusOutlined /></template>
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="variables"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 'max-content' }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'ordinal'">
          {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
        </template>
        <template v-else-if="column.key === 'value'">
          <span class="value-preview">{{ record.value || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-tag :color="record.enabled === false ? 'default' : 'green'">
            {{ record.enabled === false ? t('globalVariable.disabled') : t('globalVariable.enabled') }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'updatedAt'">
          {{ formatTimestamp(record.updatedAt) }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button size="small" @click="openEdit(record)">{{ t('globalVariable.edit') }}</a-button>
            <a-button size="small" danger :title="t('globalVariable.delete')" @click="openDelete(record)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="formModalOpen"
      :title="modalTitleText"
      :confirm-loading="formModalLoading"
      :mask-closable="false"
      @ok="submitForm"
      @cancel="closeForm"
    >
      <template #footer>
        <a-space>
          <a-button :title="t('common.cancel')" @click="closeForm">
            <template #icon><UndoOutlined /></template>
          </a-button>
          <a-button type="primary" :loading="formModalLoading" :title="t('common.save')" @click="submitForm">
            <template #icon><SaveOutlined /></template>
          </a-button>
        </a-space>
      </template>
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        layout="horizontal"
        style="margin-top: 8px"
      >
        <a-form-item :label="t('globalVariable.key')" name="key">
          <a-input
            v-model:value="formData.key"
            :placeholder="t('globalVariable.keyPlaceholder')"
            :disabled="isFormEdit"
          />
        </a-form-item>
        <a-form-item :label="t('globalVariable.name')" name="name">
          <a-input v-model:value="formData.name" :placeholder="t('globalVariable.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('globalVariable.value')" name="value">
          <a-textarea v-model:value="formData.value" :rows="4" :placeholder="t('globalVariable.valuePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('globalVariable.description')" name="description">
          <a-textarea v-model:value="formData.description" :rows="3" :placeholder="t('globalVariable.descriptionPlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('globalVariable.enabled')" name="enabled">
          <a-switch v-model:checked="formData.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="deleteModal.open"
      :title="t('globalVariable.deleteConfirmTitle')"
      :confirm-loading="deleteModal.loading"
      ok-type="danger"
      @ok="confirmDelete"
      @cancel="closeDelete"
    >
      <template #footer>
        <a-space>
          <a-button :title="t('common.cancel')" @click="closeDelete">
            <template #icon><UndoOutlined /></template>
          </a-button>
          <a-button type="primary" danger :loading="deleteModal.loading" :title="t('globalVariable.delete')" @click="confirmDelete">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </a-space>
      </template>
      <p>
        {{ t('globalVariable.deleteConfirmPrefix') }}
        <strong>{{ deleteModal.target?.name }}</strong>
        {{ t('globalVariable.deleteConfirmSuffix') }}
      </p>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, ReloadOutlined, SaveOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { useFormModal } from '../composables/useFormModal'
import { formatTimestamp } from '../utils/time'
import {
  createGlobalVariable,
  deleteGlobalVariable,
  fetchGlobalVariablesPage,
  updateGlobalVariable
} from '../utils/globalVariableApi'

const { t } = useI18n()

const loading = ref(false)
const variables = ref([])
const searchText = ref('')
const formRef = ref(null)
const formModalLoading = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
})

const { open: formModalOpen, currentId: formEditId, openCreate: openFormCreate, openEdit: openFormEdit, close: closeFormModal } =
  useFormModal()

const isFormEdit = computed(() => !!formEditId.value)
const modalTitleText = computed(() => (isFormEdit.value ? t('globalVariable.editTitle') : t('globalVariable.createTitle')))

const formData = reactive({
  key: '',
  name: '',
  value: '',
  description: '',
  enabled: true
})

const deleteModal = reactive({
  open: false,
  loading: false,
  target: null
})

const formRules = computed(() => ({
  key: [{ required: true, message: t('globalVariable.keyRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('globalVariable.nameRequired'), trigger: 'blur' }],
  value: [{ required: true, message: t('globalVariable.valueRequired'), trigger: 'blur' }]
}))

const columns = computed(() => [
  { title: t('globalVariable.ordinal'), key: 'ordinal', width: 72 },
  { title: t('globalVariable.key'), dataIndex: 'key', key: 'key' },
  { title: t('globalVariable.name'), dataIndex: 'name', key: 'name' },
  { title: t('globalVariable.value'), dataIndex: 'value', key: 'value', width: 280 },
  { title: t('globalVariable.status'), key: 'enabled', width: 100 },
  { title: t('globalVariable.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: t('globalVariable.actions'), key: 'actions', width: 140, fixed: 'right' }
])

const handleTableChange = (pager) => {
  pagination.current = pager.current || 1
  loadVariables()
}

const onSearch = () => {
  pagination.current = 1
  loadVariables()
}

const resetQuery = () => {
  searchText.value = ''
  pagination.current = 1
  loadVariables()
}

const loadVariables = async () => {
  loading.value = true
  try {
    const page = await fetchGlobalVariablesPage({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchText.value.trim()
    })
    variables.value = page.items || []
    pagination.total = page.total || 0
  } catch (e) {
    message.error(e?.message || t('globalVariable.loadFailed'))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  openFormCreate(() => {
    formData.key = ''
    formData.name = ''
    formData.value = ''
    formData.description = ''
    formData.enabled = true
  })
}

const openEdit = (record) => {
  openFormEdit(record, (r) => {
    formData.key = r.key
    formData.name = r.name
    formData.value = r.value
    formData.description = r.description || ''
    formData.enabled = r.enabled !== false
  })
}

const closeForm = () => {
  closeFormModal()
  formRef.value?.resetFields()
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return Promise.reject(new Error('validation'))
  }
  formModalLoading.value = true
  try {
    const payload = {
      key: formData.key,
      name: formData.name,
      value: formData.value,
      description: formData.description,
      enabled: formData.enabled
    }
    if (isFormEdit.value) {
      await updateGlobalVariable(formEditId.value, payload)
      message.success(t('globalVariable.updated'))
    } else {
      await createGlobalVariable(payload)
      message.success(t('globalVariable.created'))
    }
    closeFormModal()
    await loadVariables()
  } catch (e) {
    message.error(e?.message || t('globalVariable.saveFailed'))
    return Promise.reject(e)
  } finally {
    formModalLoading.value = false
  }
}

const openDelete = (record) => {
  deleteModal.target = record
  deleteModal.open = true
}

const closeDelete = () => {
  deleteModal.open = false
  deleteModal.target = null
}

const confirmDelete = async () => {
  if (!deleteModal.target) return
  deleteModal.loading = true
  try {
    await deleteGlobalVariable(deleteModal.target.id)
    message.success(t('globalVariable.deleted'))
    closeDelete()
    await loadVariables()
  } catch (e) {
    message.error(e?.message || t('globalVariable.deleteFailed'))
  } finally {
    deleteModal.loading = false
  }
}

onMounted(loadVariables)
</script>

<style scoped>
.global-variable-layout {
  padding: 24px;
  border-radius: 16px;
}

.global-variable-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.global-variable-header h2 {
  margin: 0;
}

.global-variable-header p {
  margin: 4px 0 0;
}

.global-variable-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.global-variable-icon-btn,
.global-variable-add-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
}

.value-preview {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

@media (max-width: 768px) {
  .global-variable-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .global-variable-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
