<template>
  <div class="login-page">
    <a-card class="login-card" bordered>
      <div class="login-header">
        <h2>后台登录</h2>
        <p>首个注册账号将自动成为管理员</p>
      </div>
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="login" tab="登录">
          <a-form :model="form" :rules="rules" layout="vertical" @finish="onSubmit">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="form.username" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="form.password" placeholder="请输入密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit" block :loading="loading">
              登录
            </a-button>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="register" tab="注册">
          <p class="register-note">首次注册的账号将自动获得管理员权限。</p>
          <a-form :model="registerForm" :rules="registerRules" layout="vertical" @finish="onRegister">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="registerForm.username" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="registerForm.password" placeholder="请输入密码" />
            </a-form-item>
            <a-form-item label="确认密码" name="confirmPassword">
              <a-input-password v-model:value="registerForm.confirmPassword" placeholder="请再次输入密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit" block :loading="registering">
              注册
            </a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { setAuth } from '../utils/auth'
import { syncAuth } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const registering = ref(false)
const activeTab = ref('login')

const form = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: (_, value) => {
        if (!value || value === registerForm.password) {
          return Promise.resolve()
        }
        return Promise.reject(new Error('两次密码不一致'))
      }
    }
  ]
}

const onSubmit = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
    if (!response.ok) {
      const errorText = await response.text()
      message.error(errorText || '登录失败')
      return
    }
    const data = await response.json()
    setAuth(data)
    syncAuth()
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    message.error('登录请求失败')
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  registering.value = true
  try {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: registerForm.username,
        password: registerForm.password
      })
    })
    if (!response.ok) {
      const errorText = await response.text()
      message.error(errorText || '注册失败')
      return
    }
    message.success('注册成功，请登录')
    form.username = registerForm.username
    form.password = ''
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    activeTab.value = 'login'
  } catch (error) {
    message.error('注册请求失败')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top, #f0f5ff, #f5f5f5 60%);
}

.login-card {
  width: 360px;
}

.login-header {
  margin-bottom: 24px;
  text-align: center;
}

.login-header h2 {
  margin-bottom: 4px;
  font-size: 22px;
  color: #1f1f1f;
}

.login-header p {
  margin: 0;
  color: #8c8c8c;
}

.register-note {
  margin: 0 0 12px;
  color: #5b5b5b;
  font-size: 13px;
}
</style>
