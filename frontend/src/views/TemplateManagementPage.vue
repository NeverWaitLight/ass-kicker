<template>
  <section class="template-layout">
    <div class="template-header">
      <div>
        <h2>模板</h2>
        <p>集中管理消息模板与多语言内容</p>
      </div>
      <div class="template-actions">
        <a-input
          v-model:value="searchText"
          placeholder="搜索模板名称或编码"
          allow-clear
          style="width: 220px"
        />
        <a-button :loading="loading" @click="loadTemplates">刷新</a-button>
        <a-button type="primary" @click="openCreate">新建</a-button>
      </div>
    </div>

    <a-alert
      v-if="error"
      type="error"
      :message="error"
      show-icon
      closable
      @close="error = ''"
      style="margin-bottom: 16px"
    />

    <a-table
      :columns="columns"
      :data-source="filteredTemplates"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'description'">
          <span class="text-muted">{{ record.description || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'applicableSenderTypes'">
          <a-tag v-for="t in (record.applicableSenderTypes || [])" :key="t" color="blue">{{ channelTypeLabel(t) }}</a-tag>
          <span v-if="!(record.applicableSenderTypes || []).length" class="text-muted">-</span>
        </template>
        <template v-else-if="column.key === 'contentType'">
          {{ getContentTypeLabel(record.contentType) }}
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTimestamp(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button size="small" @click="goDetail(record)">详情</a-button>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" danger @click="openDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="formModal.open"
      :title="formModal.isEdit ? '编辑模板' : '新建模板'"
      :confirm-loading="formModal.loading"
      @ok="submitForm"
      @cancel="closeForm"
      ok-text="保存"
      cancel-text="取消"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
        style="margin-top: 8px"
      >
        <a-form-item label="模板名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入模板名称" />
        </a-form-item>
        <a-form-item label="模板编码" name="code">
          <a-input
            v-model:value="formData.code"
            placeholder="请输入模板编码，如 WELCOME_SMS"
            :disabled="formModal.isEdit"
          />
        </a-form-item>
        <a-form-item label="适用通道类型" name="applicableSenderTypes">
          <a-select
            v-model:value="formData.applicableSenderTypes"
            mode="multiple"
            placeholder="选择适用的通道类型（可多选）"
            :options="channelTypeOptions"
            allow-clear
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="内容类型" name="contentType">
          <a-select
            v-model:value="formData.contentType"
            placeholder="选择内容类型"
            :options="CONTENT_TYPE_OPTIONS"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入描述（可选）"
            :rows="3"
            :maxlength="1000"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认弹窗 -->
    <a-modal
      v-model:open="deleteModal.open"
      title="确认删除"
      :confirm-loading="deleteModal.loading"
      ok-type="danger"
      ok-text="删除"
      cancel-text="取消"
      @ok="confirmDelete"
      @cancel="closeDelete"
    >
      <p>
        确定要删除模板
        <strong>{{ deleteModal.target?.name }}</strong>
        吗？此操作不可恢复，同时会删除所有关联的语言内容。
      </p>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { formatTimestamp } from '../utils/time'
import {
  fetchTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate
} from '../utils/templateApi'
import { CHANNEL_TYPE_VALUES, CHANNEL_TYPE_LABELS } from '../constants/channelTypes'
import { CONTENT_TYPE_OPTIONS, getContentTypeLabel } from '../constants/templateTypes'

const router = useRouter()

const channelTypeOptions = CHANNEL_TYPE_VALUES.map((v) => ({
  value: v,
  label: CHANNEL_TYPE_LABELS['zh-CN'][v] || v
}))

const channelTypeLabel = (value) => CHANNEL_TYPE_LABELS['zh-CN'][value] || value

const loading = ref(false)
const error = ref('')
const templates = ref([])
const searchText = ref('')
const formRef = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
})

const formModal = reactive({
  open: false,
  isEdit: false,
  loading: false,
  editId: null
})

const deleteModal = reactive({
  open: false,
  loading: false,
  target: null
})

const formData = reactive({
  name: '',
  code: '',
  description: '',
  applicableSenderTypes: [],
  contentType: 'PLAIN_TEXT'
})

const formRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }]
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '编码', dataIndex: 'code', key: 'code', ellipsis: true },
  { title: '适用通道', key: 'applicableSenderTypes', width: 180 },
  { title: '内容类型', key: 'contentType', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' }
]

const filteredTemplates = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return templates.value
  return templates.value.filter(
    (t) =>
      t.name?.toLowerCase().includes(kw) ||
      t.code?.toLowerCase().includes(kw)
  )
})

const handleTableChange = (pager) => {
  pagination.current = pager.current || 1
}

const loadTemplates = async () => {
  loading.value = true
  error.value = ''
  try {
    templates.value = await fetchTemplates()
    pagination.total = templates.value.length
  } catch (e) {
    error.value = e?.message || '获取模板列表失败'
  } finally {
    loading.value = false
  }
}

const goDetail = (record) => {
  router.push(`/templates/${record.id}`)
}

const openCreate = () => {
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.applicableSenderTypes = []
  formData.contentType = 'PLAIN_TEXT'
  formModal.isEdit = false
  formModal.editId = null
  formModal.open = true
}

const openEdit = (record) => {
  formData.name = record.name
  formData.code = record.code
  formData.description = record.description || ''
  formData.applicableSenderTypes = record.applicableSenderTypes ? [...record.applicableSenderTypes] : []
  formData.contentType = record.contentType || 'PLAIN_TEXT'
  formModal.isEdit = true
  formModal.editId = record.id
  formModal.open = true
}

const closeForm = () => {
  formModal.open = false
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  formModal.loading = true
  try {
    const payload = {
      name: formData.name,
      code: formData.code,
      description: formData.description,
      applicableSenderTypes: formData.applicableSenderTypes,
      contentType: formData.contentType
    }
    if (formModal.isEdit) {
      await updateTemplate(formModal.editId, payload)
      message.success('模板已更新')
    } else {
      await createTemplate(payload)
      message.success('模板已创建')
    }
    formModal.open = false
    await loadTemplates()
  } catch (e) {
    message.error(e?.message || '操作失败')
  } finally {
    formModal.loading = false
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
    await deleteTemplate(deleteModal.target.id)
    message.success('模板已删除')
    closeDelete()
    await loadTemplates()
  } catch (e) {
    message.error(e?.message || '删除失败')
  } finally {
    deleteModal.loading = false
  }
}

onMounted(loadTemplates)
</script>

<style scoped>
.template-layout {
  padding: 24px;
  border-radius: 16px;
}

.template-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.template-header h2 {
  margin: 0;
}

.template-header p {
  margin: 4px 0 0;
}

.template-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}


@media (max-width: 768px) {
  .template-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .template-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
