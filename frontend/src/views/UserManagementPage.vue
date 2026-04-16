<template>
  <section class="data-list-page">
    <header class="data-list-page__header">
      <div>
        <h2 class="data-list-page__title">用户管理</h2>
        <p class="data-list-page__desc">管理系统账号、角色、状态与密码</p>
      </div>
    </header>

    <a-card class="data-list-card" :bordered="false">
      <template #title>
        <div class="data-list-card__head">
          <span class="data-list-card__head-title">用户列表</span>
          <span class="data-list-card__head-meta">共 {{ pagination.total }} 人</span>
        </div>
      </template>
      <template #extra>
        <a-space wrap>
          <a-input
            v-model:value="keyword"
            allow-clear
            placeholder="搜索用户名"
            class="data-list-toolbar__search"
            @pressEnter="onSearch"
          />
          <a-button :loading="loading" @click="loadUsers">刷新</a-button>
          <a-button type="primary" @click="openCreate">新增</a-button>
        </a-space>
      </template>

      <a-table
        class="data-list-table"
        :data-source="users"
        :columns="columns"
        :pagination="tablePagination"
        :loading="loading"
        :scroll="{ x: 880 }"
        table-layout="fixed"
        size="middle"
        bordered
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'ordinal'">
            <span class="cell-ordinal">{{ (pagination.page - 1) * pagination.size + index + 1 }}</span>
          </template>
          <template v-else-if="column.key === 'username'">
            <span class="data-list-cell-ellipsis" :title="record.username">{{ record.username }}</span>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'blue'">{{ roleLabel(record.role) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'red'">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            <span class="data-list-cell-time">{{ formatTimestamp(record.createdAt) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space :size="4" wrap>
              <a-button type="link" size="small" class="data-list-action-link" @click="openEdit(record)">编辑</a-button>
              <a-button type="link" size="small" class="data-list-action-link" @click="openReset(record)">重置密码</a-button>
              <a-button type="link" size="small" danger @click="confirmDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="createOpen"
      :title="createModalTitle"
      :confirm-loading="creating"
      :mask-closable="false"
      destroy-on-close
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
          <a-input v-model:value="createForm.username" autocomplete="off" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="createForm.password" autocomplete="new-password" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="createForm.role" style="width: 100%">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="MEMBER">成员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="createForm.status" style="width: 100%">
            <a-select-option value="ACTIVE">启用</a-select-option>
            <a-select-option value="DISABLED">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="editOpen"
      title="编辑"
      :confirm-loading="editing"
      :mask-closable="false"
      destroy-on-close
      @ok="submitEdit"
      @cancel="closeEdit"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        layout="horizontal"
      >
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="editForm.username" autocomplete="off" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="editForm.role" style="width: 100%">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="MEMBER">成员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="editForm.status" style="width: 100%">
            <a-select-option value="ACTIVE">启用</a-select-option>
            <a-select-option value="DISABLED">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="resetVisible"
      title="重置密码"
      :confirm-loading="resetting"
      :mask-closable="false"
      destroy-on-close
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
          <a-input-password v-model:value="resetForm.newPassword" autocomplete="new-password" />
        </a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
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
  role: 'MEMBER',
  status: 'ACTIVE'
})

const createFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const editOpen = ref(false)
const editing = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: '',
  username: '',
  role: 'MEMBER',
  status: 'ACTIVE'
})

const editFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
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

const roleLabel = (role) => {
  if (role === 'ADMIN') return '管理员'
  if (role === 'MEMBER' || role === 'USER') return '成员'
  return role || '-'
}

const normalizeRole = (role) => {
  if (role === 'USER') return 'MEMBER'
  return role || 'MEMBER'
}

const columns = [
  { title: '序号', key: 'ordinal', width: 72, align: 'center' },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 200, ellipsis: true },
  { title: '角色', dataIndex: 'role', key: 'role', width: 112, align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 112, align: 'center' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 188 },
  { title: '操作', key: 'actions', width: 220, align: 'center', fixed: 'right' }
]

const tablePagination = computed(() => ({
  current: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total) => `共 ${total} 条`
}))

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

const onSearch = () => {
  pagination.page = 1
  loadUsers()
}

const handleTableChange = (pag) => {
  pagination.page = pag.current
  pagination.size = pag.pageSize
  loadUsers()
}

const openCreate = () => {
  openCreateModal(() => {
    createForm.username = ''
    createForm.password = ''
    createForm.role = 'MEMBER'
    createForm.status = 'ACTIVE'
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
      body: JSON.stringify(createForm)
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

const openEdit = (record) => {
  editForm.id = record.id
  editForm.username = record.username
  editForm.role = normalizeRole(record.role)
  editForm.status = record.status
  editOpen.value = true
}

const closeEdit = () => {
  editOpen.value = false
  editFormRef.value?.resetFields()
}

const submitEdit = async () => {
  try {
    await editFormRef.value?.validate()
  } catch {
    return Promise.reject(new Error('validation'))
  }
  editing.value = true
  try {
    const response = await apiFetch('/v1/users', {
      method: 'PATCH',
      body: JSON.stringify({
        id: editForm.id,
        username: editForm.username,
        role: editForm.role,
        status: editForm.status
      })
    })
    if (!response.ok) {
      message.error(await response.text())
      return Promise.reject(new Error('http'))
    }
    message.success('用户已更新')
    editOpen.value = false
    editFormRef.value?.resetFields()
    await loadUsers()
  } catch (error) {
    message.error('更新用户失败')
    return Promise.reject(error)
  } finally {
    editing.value = false
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
/* 序号列略窄时仍居中 */
.cell-ordinal {
  display: inline-block;
  min-width: 1.5em;
  text-align: center;
}
</style>
