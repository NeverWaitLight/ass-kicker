<template>
  <section class="page-shell">
    <div class="page-header">
      <div>
        <h2>用户管理</h2>
        <p>管理系统账号、重置密码与控制状态</p>
      </div>
      <a-space>
        <a-input v-model:value="keyword" placeholder="搜索用户名" allow-clear @pressEnter="loadUsers" />
        <a-tooltip title="新建用户">
          <a-button type="primary" @click="openCreate">新建</a-button>
        </a-tooltip>
      </a-space>
    </div>

    <a-table
      :data-source="users"
      :columns="columns"
      :pagination="tablePagination"
      :loading="loading"
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
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTimestamp(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button size="small" @click="openReset(record)">重置密码</a-button>
            <a-button size="small" danger @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="createVisible" title="新建用户" @ok="submitCreate" :confirm-loading="creating">
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="createForm.username" />
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password v-model:value="createForm.password" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="createForm.role">
            <a-select-option value="ADMIN">ADMIN</a-select-option>
            <a-select-option value="USER">USER</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="createForm.status">
            <a-select-option value="ACTIVE">ACTIVE</a-select-option>
            <a-select-option value="DISABLED">DISABLED</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="resetVisible" title="重置密码" @ok="submitReset" :confirm-loading="resetting">
      <a-form :model="resetForm" layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model:value="resetForm.newPassword" />
        </a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { apiFetch } from '../utils/api'
import { formatTimestamp } from '../utils/time'

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  role: 'USER',
  status: 'ACTIVE'
})

const resetVisible = ref(false)
const resetting = ref(false)
const resetForm = reactive({
  userId: null,
  newPassword: ''
})

const columns = [
  { title: '序号', key: 'ordinal', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '角色', dataIndex: 'role', key: 'role', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 180 }
]

const tablePagination = computed(() => ({
  current: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  showSizeChanger: false
}))

const loadUsers = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: pagination.page.toString(),
      size: pagination.size.toString(),
      keyword: keyword.value || ''
    })
    const response = await apiFetch(`/api/users?${params.toString()}`)
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    const data = await response.json()
    users.value = data.items || []
    pagination.total = data.total || 0
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
  createVisible.value = true
  createForm.username = ''
  createForm.password = ''
  createForm.role = 'USER'
  createForm.status = 'ACTIVE'
}

const submitCreate = async () => {
  if (!createForm.username || !createForm.password) {
    message.warning('请填写用户名和密码')
    return
  }
  creating.value = true
  try {
    const response = await apiFetch('/api/users', {
      method: 'POST',
      body: JSON.stringify(createForm)
    })
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    message.success('用户已创建')
    createVisible.value = false
    await loadUsers()
  } catch (error) {
    message.error('创建用户失败')
  } finally {
    creating.value = false
  }
}

const openReset = (record) => {
  resetForm.userId = record.id
  resetForm.newPassword = ''
  resetVisible.value = true
}

const submitReset = async () => {
  if (!resetForm.newPassword) {
    message.warning('请输入新密码')
    return
  }
  resetting.value = true
  try {
    const response = await apiFetch(`/api/users/${resetForm.userId}/password`, {
      method: 'PUT',
      body: JSON.stringify({ newPassword: resetForm.newPassword })
    })
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    message.success('密码已重置')
    resetVisible.value = false
  } catch (error) {
    message.error('重置密码失败')
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
      const response = await apiFetch(`/api/users/${record.id}`, { method: 'DELETE' })
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
  color: var(--text-on-surface);
}

.page-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
}
</style>
