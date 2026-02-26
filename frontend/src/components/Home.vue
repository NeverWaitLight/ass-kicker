<template>
  <section class="hero">
    <a-typography-title :level="1" class="hero-title">发送消息</a-typography-title>
    <a-card class="hero-card" bordered>
      <a-form layout="vertical" :label-col="{ span: 24 }">
        <a-form-item label="模板" required>
          <a-select
            v-model:value="form.templateCode"
            placeholder="请选择模板"
            :loading="templatesLoading"
            :options="templateOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="语言" required>
          <a-select
            v-model:value="form.language"
            placeholder="请选择语言"
            :options="languageOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="模板参数">
          <a-textarea
            v-model:value="form.paramsText"
            placeholder='{"key":"value"}'
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="通道" required>
          <a-select
            v-model:value="form.channelId"
            placeholder="请选择通道"
            :loading="channelsLoading"
            :options="channelOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="收件人" required>
          <a-select
            v-model:value="form.recipients"
            mode="tags"
            placeholder="输入后回车添加，至少一个收件人"
            :options="[]"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="submitLoading" @click="onSubmit">提交</a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <a-alert
        v-if="lastTaskId"
        type="success"
        :message="`任务已提交，任务ID: ${lastTaskId}`"
        show-icon
        closable
        style="margin-top: 16px"
        @close="lastTaskId = ''"
      />
    </a-card>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { fetchTemplates } from '../utils/templateApi'
import { fetchChannels } from '../utils/channelApi'
import { submitSendTask } from '../utils/submitApi'

const LANGUAGE_OPTIONS = [
  { label: '简体中文', value: 'ZH_HANS' },
  { label: '繁体中文', value: 'ZH_HANT' },
  { label: 'English', value: 'EN' },
  { label: 'Français', value: 'FR' },
  { label: 'Deutsch', value: 'DE' }
]

const form = ref({
  templateCode: undefined,
  language: undefined,
  paramsText: '',
  channelId: undefined,
  recipients: []
})
const templates = ref([])
const channels = ref([])
const templatesLoading = ref(false)
const channelsLoading = ref(false)
const submitLoading = ref(false)
const lastTaskId = ref('')

const templateOptions = computed(() =>
  templates.value.map((t) => ({ label: t.name || t.code, value: t.code }))
)
const channelOptions = computed(() =>
  channels.value.map((c) => ({ label: c.name, value: c.id }))
)
const languageOptions = computed(() => LANGUAGE_OPTIONS)

async function loadTemplates() {
  templatesLoading.value = true
  try {
    templates.value = await fetchTemplates()
  } catch (e) {
    message.error('加载模板失败: ' + (e.message || String(e)))
  } finally {
    templatesLoading.value = false
  }
}

async function loadChannels() {
  channelsLoading.value = true
  try {
    channels.value = await fetchChannels()
  } catch (e) {
    message.error('加载通道失败: ' + (e.message || String(e)))
  } finally {
    channelsLoading.value = false
  }
}

function parseParams() {
  const text = form.value.paramsText?.trim()
  if (!text) return {}
  try {
    const parsed = JSON.parse(text)
    return typeof parsed === 'object' && parsed !== null ? parsed : {}
  } catch {
    return {}
  }
}

async function onSubmit() {
  const { templateCode, language, channelId, recipients } = form.value
  if (!templateCode) {
    message.warning('请选择模板')
    return
  }
  if (!language) {
    message.warning('请选择语言')
    return
  }
  if (!channelId) {
    message.warning('请选择通道')
    return
  }
  const recipientList = Array.isArray(recipients)
    ? recipients.filter((r) => r != null && String(r).trim() !== '')
    : []
  if (recipientList.length === 0) {
    message.warning('请至少填写一个收件人')
    return
  }
  const params = parseParams()
  if (form.value.paramsText?.trim() && Object.keys(params).length === 0) {
    message.warning('模板参数不是合法 JSON，已按空对象提交')
  }
  submitLoading.value = true
  try {
    const data = await submitSendTask({
      templateCode,
      language,
      params,
      channelId,
      recipients: recipientList
    })
    lastTaskId.value = data.taskId || ''
    message.success('任务已提交，任务ID: ' + (data.taskId || ''))
  } catch (e) {
    message.error('提交失败: ' + (e.message || String(e)))
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadChannels()
})
</script>

<style scoped>
.hero {
  max-width: 640px;
}
.hero-title {
  margin-bottom: 24px;
}
.hero-card {
  margin-top: 0;
}
</style>
