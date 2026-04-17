<template>
  <div>
    <a-modal
      v-model:open="profileOpen"
      :title="t('user.personalSettings')"
      :footer="null"
      centered
      :width="480"
      destroy-on-close
    >
      <a-form :model="usernameForm" layout="vertical">
        <a-form-item :label="t('user.newUsername')">
          <a-input v-model:value="usernameForm.username" />
        </a-form-item>
        <a-button type="primary" :loading="updatingName" @click="submitUsername">
          {{ t('common.save') }}
        </a-button>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="passwordOpen"
      :title="t('user.changePassword')"
      :footer="null"
      centered
      :width="480"
      destroy-on-close
    >
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item :label="t('user.oldPassword')">
          <a-input-password v-model:value="passwordForm.oldPassword" />
        </a-form-item>
        <a-form-item :label="t('user.newPassword')">
          <a-input-password v-model:value="passwordForm.newPassword" />
        </a-form-item>
        <a-button type="primary" :loading="updatingPassword" @click="submitPassword">
          {{ t('common.save') }}
        </a-button>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="apiKeyOpen"
      :title="t('user.apiKeyManagement')"
      :footer="null"
      centered
      :width="720"
      destroy-on-close
    >
      <a-form :model="apiKeyForm" layout="inline" style="margin-bottom: 16px">
        <a-form-item :label="t('user.apiKeyName')">
          <a-input
            v-model:value="apiKeyForm.name"
            :placeholder="t('user.apiKeyNamePlaceholder')"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="creatingKey" @click="handleCreateApiKey">
            {{ t('user.createApiKey') }}
          </a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="apiKeys"
        :columns="apiKeyColumns"
        :loading="loadingKeys"
        row-key="id"
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
            <a-popconfirm
              :title="t('user.revokeApiKeyConfirm')"
              :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')"
              @confirm="handleRevokeApiKey(record.id)"
            >
              <a-button type="link" danger size="small">{{ t('user.revokeApiKey') }}</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal
      v-model:open="newKeyModalVisible"
      :title="t('user.apiKeyCreated')"
      :footer="null"
      centered
      :mask-closable="false"
    >
      <a-alert
        type="warning"
        :message="t('user.apiKeySaveHint')"
        style="margin-bottom: 16px"
        show-icon
      />
      <a-input-group compact>
        <a-input :value="newKeyValue" readonly style="width: calc(100% - 80px)" />
        <a-button @click="copyKey">{{ t('common.copy') }}</a-button>
      </a-input-group>
      <div style="margin-top: 16px; text-align: right">
        <a-button type="primary" @click="newKeyModalVisible = false">{{ t('user.apiKeySaved') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { unwrapData } from '../utils/apiPayload'
import { apiFetch } from '../utils/v1'
import { setAuth } from '../utils/auth'
import { currentUser, syncAuth } from '../stores/auth'
import { createApiKey, listApiKeys, revokeApiKey } from '../utils/apiKeyApi'

const { t } = useI18n()

const profileOpen = ref(false)
const passwordOpen = ref(false)
const apiKeyOpen = ref(false)

const usernameForm = reactive({
  username: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const updatingName = ref(false)
const updatingPassword = ref(false)

const onProfileOpen = () => {
  usernameForm.username = currentUser.value?.username || ''
}

const onPasswordOpen = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
}

const onApiKeyOpen = () => {
  loadApiKeys()
}

watch(profileOpen, (open) => {
  if (open) onProfileOpen()
})
watch(passwordOpen, (open) => {
  if (open) onPasswordOpen()
})
watch(apiKeyOpen, (open) => {
  if (open) onApiKeyOpen()
})

const submitUsername = async () => {
  if (!usernameForm.username) {
    message.warning(t('user.usernameRequired'))
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
    message.success(t('user.usernameUpdated'))
    profileOpen.value = false
  } catch {
    message.error(t('user.usernameUpdateFailed'))
  } finally {
    updatingName.value = false
  }
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning(t('user.passwordFieldsRequired'))
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
    message.success(t('user.passwordUpdated'))
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordOpen.value = false
  } catch {
    message.error(t('user.passwordUpdateFailed'))
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

const apiKeyColumns = computed(() => [
  { title: t('user.apiKeyColName'), dataIndex: 'name', key: 'name' },
  { title: t('user.apiKeyColPrefix'), key: 'maskedRawKey' },
  { title: t('user.apiKeyColCreated'), key: 'createdAt' },
  { title: t('user.apiKeyColAction'), key: 'action' }
])

const loadApiKeys = async () => {
  loadingKeys.value = true
  try {
    const data = await listApiKeys()
    apiKeys.value = Array.isArray(data) ? data : []
  } catch {
    message.error(t('user.apiKeyListFailed'))
  } finally {
    loadingKeys.value = false
  }
}

const handleCreateApiKey = async () => {
  creatingKey.value = true
  try {
    const name = apiKeyForm.name.trim() || t('user.defaultApiKeyName')
    const data = await createApiKey({ name })
    newKeyValue.value = data.rawKey
    newKeyModalVisible.value = true
    apiKeyForm.name = ''
    await loadApiKeys()
  } catch {
    message.error(t('user.apiKeyCreateFailed'))
  } finally {
    creatingKey.value = false
  }
}

const handleRevokeApiKey = async (id) => {
  try {
    await revokeApiKey(id)
    message.success(t('user.apiKeyRevoked'))
    await loadApiKeys()
  } catch {
    message.error(t('user.apiKeyRevokeFailed'))
  }
}

const copyKey = () => {
  navigator.clipboard.writeText(newKeyValue.value).then(() => {
    message.success(t('common.copied'))
  })
}

const openProfile = () => {
  profileOpen.value = true
}

const openPassword = () => {
  passwordOpen.value = true
}

const openApiKeys = () => {
  apiKeyOpen.value = true
}

defineExpose({
  openProfile,
  openPassword,
  openApiKeys
})
</script>
