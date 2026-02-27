<template>
  <section class="page-shell">
    <div class="page-header">
      <div>
        <h2>发送记录</h2>
        <p>分页浏览消息发送记录</p>
      </div>
      <a-button :loading="loading" @click="loadRecords">刷新</a-button>
    </div>

    <div class="search-bar">
      <a-select
        v-model:value="channelTypeFilter"
        placeholder="通道类型"
        allow-clear
        class="filter-select"
        :options="channelTypeOptions"
        @change="doSearch"
      />
      <a-input-search
        v-model:value="recipientSearch"
        placeholder="输入邮箱/手机号等精准搜索"
        enter-button="搜索"
        allow-clear
        class="search-input"
        @search="doSearch"
      />
    </div>

    <a-table
      :data-source="records"
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
        <template v-else-if="column.key === 'channelType'">
          <a-tag color="blue">{{ channelTypeLabel(record.channelType) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'recipient'">
          {{ record.recipient || (record.recipients && record.recipients[0]) || '-' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'submittedAt'">
          {{ formatTimestamp(record.submittedAt) }}
        </template>
        <template v-else-if="column.key === 'sentAt'">
          {{ formatTimestamp(record.sentAt) }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button size="small" @click="goDetail(record)">详情</a-button>
        </template>
      </template>
    </a-table>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listSendRecords } from '../utils/sendRecordApi'
import { formatTimestamp } from '../utils/time'
import { CHANNEL_TYPE_LABELS, CHANNEL_TYPE_VALUES } from '../constants/channelTypes'

const router = useRouter()
const records = ref([])
const loading = ref(false)
const recipientSearch = ref('')
const channelTypeFilter = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })

const channelTypeOptions = [
  { value: '', label: '全部' },
  ...CHANNEL_TYPE_VALUES.map((v) => ({ value: v, label: CHANNEL_TYPE_LABELS['zh-CN'][v] || v }))
]

const columns = [
  { title: '序号', key: 'ordinal', width: 70 },
  { title: '任务ID', dataIndex: 'taskId', key: 'taskId', ellipsis: true, width: 140 },
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', width: 120 },
  { title: '通道', dataIndex: 'channelName', key: 'channelName', width: 120 },
  { title: '通道类型', key: 'channelType', width: 100 },
  { title: '接收人', key: 'recipient', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '提交时间', key: 'submittedAt', width: 170 },
  { title: '发送时间', key: 'sentAt', width: 170 },
  { title: '操作', key: 'actions', width: 90 }
]

const tablePagination = computed(() => ({
  current: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  showSizeChanger: false
}))

const channelTypeLabel = (value) => (value ? (CHANNEL_TYPE_LABELS['zh-CN'][value] || value) : '-')

const statusColor = (status) => {
  if (status === 'SUCCESS') return 'green'
  if (status === 'FAILED') return 'red'
  if (status === 'PENDING') return 'blue'
  return 'default'
}

const statusLabel = (status) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'PENDING') return '处理中'
  return status || '-'
}

const loadRecords = async () => {
  loading.value = true
  try {
    const recipient = recipientSearch.value != null ? String(recipientSearch.value).trim() || undefined : undefined
    const channelType = channelTypeFilter.value != null && String(channelTypeFilter.value).trim() !== '' ? String(channelTypeFilter.value).trim() : undefined
    const data = await listSendRecords(pagination.page, pagination.size, recipient, channelType)
    records.value = data.items || []
    pagination.total = data.total ?? 0
  } catch (error) {
    message.error(error?.message || '获取发送记录失败')
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  pagination.page = 1
  loadRecords()
}

const handleTableChange = (pager) => {
  pagination.page = pager.current
  loadRecords()
}

const goDetail = (record) => {
  router.push(`/send-records/${record.id}`)
}

onMounted(loadRecords)
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

.search-bar {
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-bar .filter-select {
  width: 140px;
}

.search-bar .search-input {
  max-width: 360px;
}
</style>
