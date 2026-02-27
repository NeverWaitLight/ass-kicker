<template>
  <section class="page-shell">
    <div class="detail-nav">
      <a-button @click="goBack">← 返回列表</a-button>
    </div>

    <a-spin :spinning="loading">
      <div v-if="record" class="info-card">
        <h2 class="info-title">发送记录详情</h2>
        <a-descriptions :column="2" bordered size="small" class="info-descriptions">
          <a-descriptions-item label="记录ID">
            <code class="desc-code">{{ record.id }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="任务ID">
            <code class="desc-code">{{ record.taskId || '-' }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="模板编码">{{ record.templateCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="语言">{{ record.languageCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="通道ID">{{ record.channelId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="通道名称">{{ record.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="通道类型">
            <a-tag v-if="record.channelType" color="blue">{{ channelTypeLabel(record.channelType) }}</a-tag>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="接收人">{{ record.recipient || (record.recipients && record.recipients.join(', ')) || '-' }}</a-descriptions-item>
          <a-descriptions-item label="提交时间">{{ formatTimestamp(record.submittedAt) }}</a-descriptions-item>
          <a-descriptions-item label="发送时间">{{ formatTimestamp(record.sentAt) }}</a-descriptions-item>
          <a-descriptions-item label="发送结果">
            <a-tag :color="record.success ? 'green' : 'red'">{{ record.success ? '成功' : '失败' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="!record.success" label="错误码">{{ record.errorCode || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="!record.success" label="错误信息">{{ record.errorMessage || '-' }}</a-descriptions-item>
          <a-descriptions-item label="模板参数" :span="2">
            <pre v-if="record.params && Object.keys(record.params).length" class="pre-block">{{ JSON.stringify(record.params, null, 2) }}</pre>
            <span v-else class="text-muted">-</span>
          </a-descriptions-item>
          <a-descriptions-item label="渲染内容" :span="2">
            <pre v-if="record.renderedContent" class="pre-block">{{ record.renderedContent }}</pre>
            <span v-else class="text-muted">-</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
      <a-empty v-else-if="!loading && error" :description="error" />
    </a-spin>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getSendRecord } from '../utils/sendRecordApi'
import { formatTimestamp } from '../utils/time'
import { CHANNEL_TYPE_LABELS } from '../constants/channelTypes'

const route = useRoute()
const router = useRouter()
const record = ref(null)
const loading = ref(true)
const error = ref('')

const channelTypeLabel = (value) => (value ? (CHANNEL_TYPE_LABELS['zh-CN'][value] || value) : '-')

const fetchRecord = async () => {
  const id = route.params.id
  if (!id) {
    error.value = '缺少记录ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  record.value = null
  try {
    record.value = await getSendRecord(id)
  } catch (e) {
    error.value = e?.message || '发送记录不存在'
    message.error(error.value)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/send-records')
}

watch(() => route.params.id, fetchRecord, { immediate: true })
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.detail-nav {
  margin-bottom: 16px;
}

.info-card {
  background: var(--ant-color-bg-container);
  border-radius: 8px;
  padding: 24px;
}

.info-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.info-descriptions {
  margin-top: 0;
}

.desc-code {
  font-size: 12px;
  background: var(--ant-color-fill-quaternary);
  padding: 2px 6px;
  border-radius: 4px;
}

.pre-block {
  margin: 0;
  padding: 12px;
  background: var(--ant-color-fill-quaternary);
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.text-muted {
  color: var(--ant-color-text-secondary);
}
</style>
