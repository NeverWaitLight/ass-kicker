<template>
  <ChannelManagementLayout>
    <template #title>{{ pageTitle }}</template>
    <template #subtitle>配置通道类型、名称与属性</template>
    <template #actions>
      <a-space>
        <a-tooltip title="返回列表">
          <a-button @click="goBack">返回</a-button>
        </a-tooltip>
        <a-button :disabled="editorRef?.testDenied ?? true" @click="() => editorRef?.openTestModal()">测试</a-button>
        <a-button
          type="primary"
          :loading="editorRef?.saving"
          :disabled="editorRef?.denied"
          title="保存"
          @click="() => editorRef?.saveChannel()"
        >
          <template #icon><SaveOutlined /></template>
        </a-button>
      </a-space>
    </template>

    <ChannelConfigEditor ref="editorRef" :channel-id="channelId" />
  </ChannelManagementLayout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SaveOutlined } from '@ant-design/icons-vue'
import ChannelManagementLayout from '../components/channels/ChannelManagementLayout.vue'
import ChannelConfigEditor from '../components/channels/ChannelConfigEditor.vue'

const route = useRoute()
const router = useRouter()
const editorRef = ref(null)

const channelId = computed(() => route.params.id || null)

const pageTitle = computed(() => (channelId.value ? '编辑通道' : '新建通道'))

const goBack = () => {
  router.push('/channels')
}
</script>
