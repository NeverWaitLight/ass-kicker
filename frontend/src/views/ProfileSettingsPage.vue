<template>
  <section class="data-list-page profile-page">
    <header class="data-list-page__header">
      <div>
        <h2 class="data-list-page__title">个人设置</h2>
        <p class="data-list-page__desc">更新用户名与密码</p>
      </div>
    </header>

    <a-row :gutter="[24, 24]">
      <a-col :xs="24" :md="12">
        <a-card title="修改用户名" bordered>
          <a-form :model="usernameForm" layout="vertical">
            <a-form-item label="新用户名">
              <a-input v-model:value="usernameForm.username" />
            </a-form-item>
            <a-button type="primary" :loading="updatingName" @click="submitUsername">保存</a-button>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card title="修改密码" bordered>
          <a-form :model="passwordForm" layout="vertical">
            <a-form-item label="旧密码">
              <a-input-password v-model:value="passwordForm.oldPassword" />
            </a-form-item>
            <a-form-item label="新密码">
              <a-input-password v-model:value="passwordForm.newPassword" />
            </a-form-item>
            <a-button type="primary" :loading="updatingPassword" @click="submitPassword">保存</a-button>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24">
        <a-card class="data-list-card api-key-card" :bordered="false">
          <template #title>
            <div class="data-list-card__head">
              <span class="data-list-card__head-title">API Key</span>
              <span class="data-list-card__head-meta">共 {{ apiKeys.length }} 个</span>
            </div>
          </template>
          <template #extra>
            <a-space wrap>
              <a-input
                v-model:value="apiKeyForm.name"
                placeholder="备注名（可选）"
                allow-clear
                class="data-list-toolbar__search"
                @pressEnter="handleCreateApiKey"
              />
              <a-button type="primary" :loading="creatingKey" @click="handleCreateApiKey">创建</a-button>
            </a-space>
          </template>

          <a-table
            class="data-list-table"
            :data-source="apiKeys"
            :columns="apiKeyColumns"
            :loading="loadingKeys"
            row-key="id"
            :pagination="false"
            table-layout="fixed"
            size="middle"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <span class="data-list-cell-ellipsis" :title="record.name">{{ record.name || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'maskedRawKey'">
                <a-typography-text code>{{ record.maskedRawKey }}</a-typography-text>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                <span class="data-list-cell-time">{{ formatApiKeyDate(record.createdAt) }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popconfirm title="确认销毁此 API Key？" ok-text="确认" cancel-text="取消" @confirm="handleRevokeApiKey(record.id)">
                  <a-button type="link" danger size="small" class="data-list-action-link">销毁</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="newKeyModalVisible"
      title="API Key 已创建"
      :footer="null"
      :maskClosable="false"
    >
      <a-alert
        type="warning"
        message="请妥善保存，此后再也无法查看完整 Key"
        style="margin-bottom: 16px"
        show-icon
      />
      <a-input-group compact>
        <a-input :value="newKeyValue" readonly style="width: calc(100% - 80px)" />
        <a-button @click="copyKey">复制</a-button>
      </a-input-group>
      <div style="margin-top: 16px; text-align: right">
        <a-button type="primary" @click="newKeyModalVisible = false">我已保存</a-button>
      </div>
    </a-modal>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { unwrapData } from '../utils/apiPayload'
import { apiFetch } from '../utils/v1'
import { setAuth } from '../utils/auth'
import { currentUser, syncAuth } from '../stores/auth'
import { createApiKey, listApiKeys, revokeApiKey } from '../utils/apiKeyApi'

const usernameForm = reactive({
  username: currentUser.value?.username || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const updatingName = ref(false)
const updatingPassword = ref(false)

const submitUsername = async () => {
  if (!usernameForm.username) {
    message.warning('请输入用户名')
    return
  }
  updatingName.value = true
  try {
    const response = await apiFetch('/v1/users/me', {
      method: 'PATCH',
      body: JSON.stringify({ username: usernameForm.username })
    })
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    const data = await response.json()
    setAuth({ user: unwrapData(data) })
    syncAuth()
    message.success('用户名已更新')
  } catch (error) {
    message.error('更新用户名失败')
  } finally {
    updatingName.value = false
  }
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning('请输入完整密码信息')
    return
  }
  updatingPassword.value = true
  try {
    const response = await apiFetch('/v1/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(passwordForm)
    })
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    message.success('密码已更新')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
  } catch (error) {
    message.error('更新密码失败')
  } finally {
    updatingPassword.value = false
  }
}

const apiKeyForm = reactive({ name: '' })
const apiKeys = ref([])
const loadingKeys = ref(false)
const creatingKey = ref(false)
const newKeyModalVisible = ref(false)
const newKeyValue = ref('')

const apiKeyColumns = [
  { title: '备注名', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: 'Key 前缀', key: 'maskedRawKey', width: 280 },
  { title: '创建时间', key: 'createdAt', width: 200 },
  { title: '操作', key: 'action', width: 100, align: 'center' }
]

const formatApiKeyDate = (ts) => {
  if (ts == null) return '-'
  return new Date(ts).toLocaleString()
}

const loadApiKeys = async () => {
  loadingKeys.value = true
  try {
    const data = await listApiKeys()
    apiKeys.value = Array.isArray(data) ? data : []
  } catch {
    message.error('获取 API Key 列表失败')
  } finally {
    loadingKeys.value = false
  }
}

const handleCreateApiKey = async () => {
  creatingKey.value = true
  try {
    const name = apiKeyForm.name.trim() || '默认密钥'
    const data = await createApiKey({ name })
    newKeyValue.value = data.rawKey
    newKeyModalVisible.value = true
    apiKeyForm.name = ''
    await loadApiKeys()
  } catch (error) {
    message.error('创建 API Key 失败')
  } finally {
    creatingKey.value = false
  }
}

const handleRevokeApiKey = async (id) => {
  try {
    await revokeApiKey(id)
    message.success('API Key 已销毁')
    await loadApiKeys()
  } catch {
    message.error('销毁 API Key 失败')
  }
}

const copyKey = () => {
  navigator.clipboard.writeText(newKeyValue.value).then(() => {
    message.success('已复制')
  })
}

onMounted(loadApiKeys)
</script>

<style scoped>
.profile-page {
  max-width: 960px;
}

.api-key-card {
  margin-top: 0;
}
</style>
