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
    </a-row>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { apiFetch } from '../utils/api'
import { setAuth } from '../utils/auth'
import { currentUser, syncAuth } from '../stores/auth'

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
    const response = await apiFetch('/api/users/me', {
      method: 'PATCH',
      body: JSON.stringify({ username: usernameForm.username })
    })
    if (!response.ok) {
      message.error(await response.text())
      return
    }
    const data = await response.json()
    setAuth({ user: data })
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
    const response = await apiFetch('/api/users/me/password', {
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
  color: var(--text-on-surface);
}

.page-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
}
</style>
