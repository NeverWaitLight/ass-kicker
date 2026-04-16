<template>
  <section class="data-list-page">
    <header class="data-list-page__header">
      <div>
        <h2 class="data-list-page__title">发送记录</h2>
        <p class="data-list-page__desc">分页浏览消息发送记录与投递状态</p>
      </div>
    </header>

    <a-card class="data-list-card" :bordered="false">
      <template #title>
        <div class="data-list-card__head">
          <span class="data-list-card__head-title">记录列表</span>
          <span class="data-list-card__head-meta">共 {{ pagination.total }} 条</span>
        </div>
      </template>
      <template #extra>
        <a-space wrap>
          <a-select
            v-model:value="channelTypeFilter"
            placeholder="通道类型"
            allow-clear
            class="send-records-filter"
            :options="channelTypeOptions"
            @change="doSearch"
          />
          <a-input
            v-model:value="recipientSearch"
            placeholder="邮箱/手机号等"
            allow-clear
            class="data-list-toolbar__search"
            @pressEnter="doSearch"
          />
          <a-button :loading="loading" @click="loadRecords">刷新</a-button>
          <a-button type="primary" @click="doSearch">搜索</a-button>
        </a-space>
      </template>

      <a-table
        class="data-list-table data-list-table--fluid"
        :data-source="records"
        :columns="columns"
        :pagination="tablePagination"
        :loading="loading"
        size="middle"
        bordered
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'ordinal'">
            {{ (pagination.page - 1) * pagination.size + index + 1 }}
          </template>
          <template v-else-if="column.key === 'taskId'">
            <span class="data-list-cell-ellipsis" :title="record.taskId">{{ record.taskId }}</span>
          </template>
          <template v-else-if="column.key === 'templateCode'">
            <span class="data-list-cell-ellipsis" :title="record.templateCode">{{ record.templateCode }}</span>
          </template>
          <template v-else-if="column.key === 'channelName'">
            <span class="data-list-cell-ellipsis" :title="record.channelName">{{ record.channelName }}</span>
          </template>
          <template v-else-if="column.key === 'channelType'">
            <a-tag color="blue">{{ channelTypeLabel(record.channelType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'recipient'">
            <span class="data-list-cell-ellipsis" :title="record.recipient">{{ record.recipient || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'submittedAt'">
            <span class="data-list-cell-time">{{ formatTimestamp(record.submittedAt) }}</span>
          </template>
          <template v-else-if="column.key === 'sentAt'">
            <span class="data-list-cell-time">{{ formatTimestamp(record.sentAt) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" class="data-list-action-link" @click="goDetail(record)">详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
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
  { value: '', label: '全部类型' },
  ...CHANNEL_TYPE_VALUES.map((v) => ({ value: v, label: CHANNEL_TYPE_LABELS['zh-CN'][v] || v }))
]

/** 不设 scroll.x，表格随容器宽度自适应；未设宽度的列参与分配剩余空间 */
const columns = [
  { title: '序号', key: 'ordinal', width: 52, align: 'center' },
  { title: '任务ID', dataIndex: 'taskId', key: 'taskId', ellipsis: true },
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', ellipsis: true },
  { title: '通道', dataIndex: 'channelName', key: 'channelName', ellipsis: true },
  { title: '通道类型', key: 'channelType', width: 96, align: 'center' },
  { title: '接收人', key: 'recipient', ellipsis: true },
  { title: '状态', key: 'status', width: 72, align: 'center' },
  { title: '提交时间', key: 'submittedAt', width: 158 },
  { title: '发送时间', key: 'sentAt', width: 158 },
  { title: '操作', key: 'actions', width: 72, align: 'center' }
]

const tablePagination = computed(() => ({
  current: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total) => `共 ${total} 条`
}))

const channelTypeLabel = (value) => (value ? (CHANNEL_TYPE_LABELS['zh-CN'][value] || value) : '-')

const statusColor = (status) => {
  if (status === 'SUCCESS') return 'green'
  if (status === 'FAILED') return 'red'
  return 'default'
}

const statusLabel = (status) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
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
  if (pager.pageSize) {
    pagination.size = pager.pageSize
  }
  loadRecords()
}

const goDetail = (record) => {
  router.push(`/send-records/${record.id}`)
}

onMounted(loadRecords)
</script>

<style scoped>
.send-records-filter {
  width: 150px;
}
</style>
