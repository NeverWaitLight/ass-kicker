<template>
  <section class="page-shell">
    <div class="page-header">
      <h2>个人设置</h2>
      <p>更新用户名与密码</p>
    </div>

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
        <a-card title="API Key 管理" bordered>
          <a-form :model="apiKeyForm" layout="inline" style="margin-bottom: 16px">
            <a-form-item label="备注名">
              <a-input v-model:value="apiKeyForm.name" placeholder="可选，默认为'默认密钥'" style="width: 200px" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="creatingKey" @click="handleCreateApiKey">创建</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :dataSource="apiKeys"
            :columns="apiKeyColumns"
            :loading="loadingKeys"
            rowKey="id"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'maskedRawKey'">
                <a-typography-text code>{{ record.maskedRawKey }}</a-typography-text>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ new Date(record.createdAt).toLocaleDateString() }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popconfirm title="确认销毁此 API Key？" ok-text="确认" cancel-text="取消" @confirm="handleRevokeApiKey(record.id)">
                  <a-button type="link" danger size="small">销毁</a-button>
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
  { title: '备注名', dataIndex: 'name', key: 'name' },
  { title: 'Key 前缀', key: 'maskedRawKey' },
  { title: '创建时间', key: 'createdAt' },
  { title: '操作', key: 'action' }
]

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
.page-shell {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-header p {
  margin: 4px 0 0;
}
</style>
