<template>
  <section class="page-shell">
    <div class="page-header">
      <div>
        <h2>用户</h2>
        <p>管理系统账号、重置密码与控制状态</p>
      </div>
      <a-space>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索用户名"
          allow-clear
          enter-button
          style="width: 220px"
          @search="onUserSearch"
        >
          <template #enterButton>
            <SearchOutlined />
          </template>
        </a-input-search>
        <a-tooltip title="新建用户">
          <a-button type="primary" title="新增" @click="openCreate">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </div>

    <a-table
      :data-source="users"
      :columns="columns"
      :pagination="tablePagination"
      :loading="loading"
      :scroll="{ x: 'max-content' }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'ordinal'">
          {{ (pagination.page - 1) * pagination.size + index + 1 }}
        </template>
        <template v-else-if="column.key === 'role'">
          <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'blue'">{{ record.role }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'red'">{{ record.status }}</a-tag>
        </template>
        <template v-else-if="column.key === 'lastLoginAt'">
          {{ record.lastLoginAt ? formatTimestamp(record.lastLoginAt) : '-' }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button size="small" title="重置密码" @click="openReset(record)">
              重置密码
            </a-button>
            <a-button size="small" danger title="删除" @click="confirmDelete(record)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="createOpen"
      :title="createModalTitle"
      :confirm-loading="creating"
      :mask-closable="false"
      @ok="submitCreate"
      @cancel="closeCreate"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="createFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        layout="horizontal"
      >
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="createForm.username" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="createForm.password" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword" :dependencies="['password']">
          <a-input-password v-model:value="createForm.confirmPassword" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="resetVisible"
      title="重置密码"
      :confirm-loading="resetting"
      :mask-closable="false"
      @ok="submitReset"
      @cancel="closeReset"
    >
      <a-form
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        layout="horizontal"
      >
        <a-form-item label="新密码" name="newPassword">
          <a-input-password v-model:value="resetForm.newPassword" />
        </a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { useFormModal } from '../composables/useFormModal'
import { unwrapPage } from '../utils/apiPayload'
import { apiFetch } from '../utils/v1'
import { formatTimestamp } from '../utils/time'

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

const { open: createOpen, modalTitle: createModalTitle, openCreate: openCreateModal, close: closeCreateModal } =
  useFormModal()

const createFormRef = ref(null)
const creating = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const createFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        if (value !== createForm.password) {
          return Promise.reject(new Error('两次输入的密码不一致'))
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

const resetVisible = ref(false)
const resetting = ref(false)
const resetFormRef = ref(null)
const resetForm = reactive({
  userId: null,
  newPassword: ''
})

const resetFormRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const columns = [
  { title: '序号', key: 'ordinal' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '角色', dataIndex: 'role', key: 'role' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '最后登录时间', dataIndex: 'lastLoginAt', key: 'lastLoginAt' },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' }
]

const tablePagination = computed(() => ({
  current: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  showSizeChanger: false
}))

const onUserSearch = () => {
  pagination.page = 1
  loadUsers()
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: pagination.page.toString(),
      size: pagination.size.toString(),
      keyword: keyword.value || ''
    })
    const response = await apiFetch(`/v1/users?${params.toString()}`)
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    const data = await response.json()
    const page = unwrapPage(data)
    users.value = page.items || []
    pagination.total = page.total || 0
  } catch (error) {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pager) => {
  pagination.page = pager.current
  loadUsers()
}

const openCreate = () => {
  openCreateModal(() => {
    createForm.username = ''
    createForm.password = ''
    createForm.confirmPassword = ''
  })
}

const closeCreate = () => {
  closeCreateModal()
  createFormRef.value?.resetFields()
}

const submitCreate = async () => {
  try {
    await createFormRef.value?.validate()
  } catch {
    return Promise.reject(new Error('validation'))
  }
  creating.value = true
  try {
    const response = await apiFetch('/v1/users', {
      method: 'POST',
      body: JSON.stringify({
        username: createForm.username,
        password: createForm.password
      })
    })
    if (!response.ok) {
      message.error(await response.text())
      return Promise.reject(new Error('http'))
    }
    message.success('用户已创建')
    closeCreateModal()
    createFormRef.value?.resetFields()
    await loadUsers()
  } catch (error) {
    message.error('创建用户失败')
    return Promise.reject(error)
  } finally {
    creating.value = false
  }
}

const openReset = (record) => {
  resetForm.userId = record.id
  resetForm.newPassword = ''
  resetVisible.value = true
}

const closeReset = () => {
  resetVisible.value = false
  resetFormRef.value?.resetFields()
}

const submitReset = async () => {
  try {
    await resetFormRef.value?.validate()
  } catch {
    return Promise.reject(new Error('validation'))
  }
  resetting.value = true
  try {
    const response = await apiFetch(`/v1/users/${resetForm.userId}/password`, {
      method: 'PUT',
      body: JSON.stringify({ newPassword: resetForm.newPassword })
    })
    if (!response.ok) {
      message.error(await response.text())
      return Promise.reject(new Error('http'))
    }
    message.success('密码已重置')
    resetVisible.value = false
    resetFormRef.value?.resetFields()
  } catch (error) {
    message.error('重置密码失败')
    return Promise.reject(error)
  } finally {
    resetting.value = false
  }
}

const confirmDelete = (record) => {
  Modal.confirm({
    title: `确认删除用户 ${record.username}？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const response = await apiFetch(`/v1/users/${record.id}`, { method: 'DELETE' })
      if (!response.ok) {
        message.error(await response.text())
        return
      }
      message.success('用户已删除')
      loadUsers()
    }
  })
}

onMounted(loadUsers)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-header p {
  margin: 4px 0 0;
}
</style>
