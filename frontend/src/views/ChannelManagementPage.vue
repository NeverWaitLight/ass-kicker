<template>
  <ChannelManagementLayout>
    <template #title>通道</template>
    <template #subtitle>集中管理通道配置、状态与权限</template>
    <template #actions>
      <a-cascader
        v-model:value="typeProviderFilter"
        :options="cascaderOptions"
        placeholder="类型 / 服务商"
        allow-clear
        expand-trigger="hover"
        change-on-select
        popup-class-name="channel-filter-cascader-dropdown"
        style="min-width: 200px"
        @change="onTypeProviderChange"
      />
      <a-input-search
        v-model:value="channelSearch"
        placeholder="搜索通道名称或类型"
        allow-clear
        enter-button
        style="width: 220px"
        @search="onSearch"
      >
        <template #enterButton>
          <SearchOutlined />
        </template>
      </a-input-search>
      <a-button :loading="channelLoading" title="刷新" @click="refreshChannels">
        <template #icon><ReloadOutlined /></template>
      </a-button>
      <a-tooltip title="新建通道">
        <a-button type="primary" :disabled="!canCreate" title="新增" @click="openCreate">
          <template #icon><PlusOutlined /></template>
        </a-button>
      </a-tooltip>
    </template>

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

    <a-modal
      v-model:open="channelEditorOpen"
      :title="channelEditorId ? '编辑' : '新增通道'"
      width="min(960px, 92vw)"
      :style="{ top: '24px' }"
      :confirm-loading="channelFormRef?.saving"
      :destroy-on-close="true"
      :mask-closable="false"
      @cancel="closeChannelEditor"
    >
      <template #footer>
        <a-space>
          <a-button title="撤销" @click="closeChannelEditor">
            <template #icon><UndoOutlined /></template>
          </a-button>
          <a-button
            type="primary"
            :loading="channelFormRef?.saving"
            title="保存"
            @click="handleChannelEditorOk"
          >
            <template #icon><SaveOutlined /></template>
          </a-button>
        </a-space>
      </template>
      <ChannelConfigEditor
        v-if="channelEditorOpen"
        :key="channelEditorKey"
        ref="channelFormRef"
        :channel-id="channelEditorId"
        embedded
        @saved="handleChannelSaved"
      />
    </a-modal>
  </ChannelManagementLayout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SaveOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons-vue'
import ChannelManagementLayout from '../components/channels/ChannelManagementLayout.vue'
import ChannelTable from '../components/channels/ChannelsPage.vue'
import ChannelDeleteModal from '../components/channels/ChannelDeleteModal.vue'
import ChannelTestSendModal from '../components/channels/ChannelTestSendModal.vue'
import ChannelConfigEditor from '../components/channels/ChannelConfigEditor.vue'
import {
  fetchChannel,
  fetchChannelTypes,
  fetchChannels,
  fetchProvidersByChannelType,
  deleteChannel
} from '../utils/channelApi'
import { CHANNEL_TYPE_LABELS, CHANNEL_TYPE_VALUES } from '../constants/channelTypes'
import {
  channelList,
  channelLoading,
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

const channelEditorOpen = ref(false)
const channelEditorId = ref(null)
const channelEditorKey = ref(0)
const channelFormRef = ref(null)

const canView = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.view))
const canCreate = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.create))
const canEdit = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.edit))
const canDelete = computed(() => hasPermission(currentUser.value, CHANNEL_PERMISSIONS.remove))
const denied = computed(() => !canView.value)
const testModalOpen = ref(false)
const testModalLoading = ref(false)
const testChannel = ref(null)

const typeProviderFilter = ref(undefined)

const buildFallbackCascaderOptions = () =>
  CHANNEL_TYPE_VALUES.map((ct) => ({
    value: ct,
    label: CHANNEL_TYPE_LABELS['zh-CN'][ct] || ct
  }))

const cascaderOptions = ref(buildFallbackCascaderOptions())

const filteredChannels = computed(() => channelList.value)

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

const normalizeChannelTypes = (types) => {
  if (!Array.isArray(types) || types.length === 0) return [...CHANNEL_TYPE_VALUES]
  return types.map((t) => (typeof t === 'string' ? t : t?.name ?? String(t)))
}

const loadCascaderOptions = async () => {
  try {
    const raw = await fetchChannelTypes()
    const list = normalizeChannelTypes(raw)
    cascaderOptions.value = await Promise.all(
      list.map(async (ct) => {
        let children = []
        try {
          const providers = await fetchProvidersByChannelType(ct)
          children = (providers || []).map((p) => ({
            value: p.value,
            label: p.label || p.value
          }))
        } catch {
          children = []
        }
        return {
          value: ct,
          label: CHANNEL_TYPE_LABELS['zh-CN'][ct] || ct,
          ...(children.length ? { children } : {})
        }
      })
    )
  } catch (error) {
    message.error(error?.message || '加载筛选选项失败')
    cascaderOptions.value = buildFallbackCascaderOptions()
  }
}

const loadChannels = async () => {
  channelLoading.value = true
  try {
    const sel = typeProviderFilter.value
    const channelType = Array.isArray(sel) && sel.length > 0 ? sel[0] : undefined
    const providerType = Array.isArray(sel) && sel.length > 1 ? sel[1] : undefined
    const data = await fetchChannels({
      page: 1,
      size: 10000,
      keyword: channelSearch.value.trim() || undefined,
      channelType,
      providerType
    })
    setChannelList(data)
  } catch (error) {
    message.error(error?.message || '获取通道列表失败')
  } finally {
    channelLoading.value = false
  }
}

const refreshChannels = async () => {
  typeProviderFilter.value = undefined
  channelSearch.value = ''
  channelPagination.page = 1
  await loadChannels()
}

const onSearch = () => {
  channelPagination.page = 1
  loadChannels()
}

const onTypeProviderChange = () => {
  channelPagination.page = 1
  loadChannels()
}

const handleTableChange = (pager) => {
  channelPagination.page = pager.current || 1
}

const openCreate = () => {
  channelEditorId.value = null
  channelEditorKey.value += 1
  channelEditorOpen.value = true
}

const openEdit = (record) => {
  if (!record?.id) return
  channelEditorId.value = record.id
  channelEditorKey.value += 1
  channelEditorOpen.value = true
}

const closeChannelEditor = () => {
  channelEditorOpen.value = false
  channelEditorId.value = null
}

const handleChannelEditorOk = async () => {
  const ok = await channelFormRef.value?.saveChannel()
  if (!ok) {
    return Promise.reject(new Error('save failed'))
  }
}

const handleChannelSaved = async () => {
  closeChannelEditor()
  await loadChannels()
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

watch(
  canView,
  async (v) => {
    if (!v) return
    await loadCascaderOptions()
    await loadChannels()
  },
  { immediate: true }
)
</script>

<style>
/* 级联面板默认固定列高 180px 选项少时底部留白 改为随内容增高并限制最大高度 */
.channel-filter-cascader-dropdown .ant-cascader-menu {
  height: auto !important;
  max-height: 180px;
  flex-grow: 0 !important;
}
</style>
