<template>
  <ChannelManagementLayout>
    <template #title>通道</template>
    <template #subtitle>集中管理通道配置、状态与权限</template>
    <template #actions>
      <a-input
        v-model:value="channelSearch"
        placeholder="搜索通道名称或类型"
        allow-clear
        style="width: 220px"
        @pressEnter="onSearch"
      />
      <a-button @click="onSearch">搜索</a-button>
      <a-button :loading="channelLoading" @click="loadChannels">刷新</a-button>
      <a-tooltip title="新建通道">
        <a-button type="primary" :disabled="!canCreate" @click="openCreate">新建</a-button>
      </a-tooltip>
    </template>

    <a-alert
      v-if="channelError"
      type="error"
      :message="channelError"
      show-icon
      closable
      @close="channelError = ''"
      style="margin-bottom: 16px"
    />

    <a-result v-if="denied" status="403" title="暂无权限" sub-title="请联系管理员开通通道权限。">
      <template #extra>
        <a-tooltip title="返回首页">
          <a-button type="primary" @click="goHome">返回</a-button>
        </a-tooltip>
      </template>
    </a-result>

    <template v-else>
      <ChannelTable
        :rows="pagedChannels"
        :loading="channelLoading"
        :pagination="tablePagination"
        :can-test="canEdit"
        :can-edit="canEdit"
        :can-delete="canDelete"
        @test="openTest"
        @edit="openEdit"
        @delete="openDelete"
        @page-change="handleTableChange"
      />
    </template>

    <ChannelDeleteModal
      :open="channelDeleteState.open"
      :loading="channelDeleteState.deleting"
      :channel="channelDeleteState.target"
      @confirm="confirmDelete"
      @cancel="closeDelete"
    />

    <ChannelTestSendModal
      :open="testModalOpen"
      :loading="testModalLoading"
      :channel-type="testChannel ? testChannel.type : ''"
      :channel-name="testChannel ? testChannel.name : ''"
      :properties="testChannel ? testChannel.properties : null"
      :disabled="!canEdit"
      @cancel="closeTestModal"
    />
  </ChannelManagementLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import ChannelManagementLayout from '../components/channels/ChannelManagementLayout.vue'
import ChannelTable from '../components/channels/ChannelTable.vue'
import ChannelDeleteModal from '../components/channels/ChannelDeleteModal.vue'
import ChannelTestSendModal from '../components/channels/ChannelTestSendModal.vue'
import { fetchChannel, fetchChannels, deleteChannel } from '../utils/channelApi'
import {
  channelList,
  channelLoading,
  channelError,
  channelPagination,
  channelSearch,
  channelDeleteState,
  setChannelList,
  removeChannel
} from '../stores/channels'
import { currentUser } from '../stores/auth'
import { CHANNEL_PERMISSIONS, hasPermission } from '../utils/permissions'
import { useRouter } from 'vue-router'

const router = useRouter()

const canView = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.view))
const canCreate = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.create))
const canEdit = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.edit))
const canDelete = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.remove))
const denied = computed(() => !canView.value)
const testModalOpen = ref(false)
const testModalLoading = ref(false)
const testChannel = ref(null)

const filteredChannels = computed(() => {
  const keyword = channelSearch.value.trim().toLowerCase()
  if (!keyword) return channelList.value
  return channelList.value.filter((channel) => {
    return (
      channel.name?.toLowerCase().includes(keyword) ||
      channel.type?.toLowerCase().includes(keyword)
    )
  })
})

const pagedChannels = computed(() => {
  const start = (channelPagination.page - 1) * channelPagination.size
  const end = start + channelPagination.size
  return filteredChannels.value.slice(start, end)
})

const tablePagination = computed(() => ({
  current: channelPagination.page,
  pageSize: channelPagination.size,
  total: filteredChannels.value.length,
  showSizeChanger: false
}))

watch(filteredChannels, (value) => {
  channelPagination.total = value.length
  const maxPage = Math.max(1, Math.ceil(value.length / channelPagination.size))
  if (channelPagination.page > maxPage) {
    channelPagination.page = maxPage
  }
})

const loadChannels = async () => {
  channelLoading.value = true
  channelError.value = ''
  try {
    const data = await fetchChannels()
    setChannelList(data)
  } catch (error) {
    channelError.value = error?.message || '获取通道列表失败'
  } finally {
    channelLoading.value = false
  }
}

const onSearch = () => {
  channelPagination.page = 1
}

const handleTableChange = (pager) => {
  channelPagination.page = pager.current || 1
}

const openCreate = () => {
  router.push('/channels/new')
}

const openEdit = (record) => {
  if (!record?.id) return
  router.push(`/channels/${record.id}`)
}

const openTest = async (record) => {
  if (!record?.id || !canEdit.value) return
  testModalLoading.value = true
  try {
    if (record.properties && record.type) {
      testChannel.value = record
    } else {
      testChannel.value = await fetchChannel(record.id)
    }
    testModalOpen.value = true
  } catch (error) {
    message.error(error?.message || '获取通道信息失败')
    testModalOpen.value = false
  } finally {
    testModalLoading.value = false
  }
}

const closeTestModal = () => {
  testModalOpen.value = false
  testChannel.value = null
}

const openDelete = (record) => {
  channelDeleteState.open = true
  channelDeleteState.target = record
}

const closeDelete = () => {
  channelDeleteState.open = false
  channelDeleteState.target = null
}

const confirmDelete = async () => {
  if (!channelDeleteState.target) return
  channelDeleteState.deleting = true
  try {
    await deleteChannel(channelDeleteState.target.id)
    removeChannel(channelDeleteState.target.id)
    message.success('通道已删除')
    closeDelete()
  } catch (error) {
    message.error(error?.message || '删除通道失败')
  } finally {
    channelDeleteState.deleting = false
  }
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  if (canView.value) {
    loadChannels()
  }
})
</script>
