<template>
  <a-modal :open="open" title="测试发送" :footer="null" @cancel="handleCancel">
    <a-spin :spinning="loading">
      <div class="test-modal">
        <a-alert
          v-if="typeError"
          type="warning"
          :message="typeError"
          show-icon
          style="margin-bottom: 12px"
        />
        <div class="test-meta" v-if="channelType">
          <a-tag color="blue">{{ displayChannelType }}</a-tag>
          <span v-if="channelName" class="test-name">{{ channelName }}</span>
        </div>
        <a-form layout="vertical" class="test-form">
          <a-form-item
            :label="targetLabel"
            required
            :validate-status="targetError ? 'error' : ''"
            :help="targetError"
          >
            <a-input v-model:value="target" :placeholder="targetPlaceholder" />
          </a-form-item>
          <a-form-item
            label="测试内容"
            required
            :validate-status="contentError ? 'error' : ''"
            :help="contentError"
          >
            <a-textarea v-model:value="content" rows="4" placeholder="请输入测试内容" />
          </a-form-item>
        </a-form>

        <div class="test-actions">
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" :loading="testing" :disabled="sendDisabled" @click="submitTestSend">
            发送
          </a-button>
        </div>

        <a-alert
          v-if="errorMessage"
          type="error"
          :message="errorMessage"
          show-icon
          closable
          @close="errorMessage = ''"
          style="margin-top: 12px"
        />
        <a-alert
          v-if="result"
          :type="result.success ? 'success' : 'error'"
          :message="resultMessage"
          show-icon
          style="margin-top: 12px"
        />
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { testSendChannel } from '../../utils/channelApi'
import { useI18n } from 'vue-i18n'
import { getChannelTypeLabel } from '../../constants/channelTypes'

const props = defineProps({
  open: { type: Boolean, default: false },
  channelType: { type: String, default: '' },
  channelName: { type: String, default: '' },
  properties: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['cancel'])

const target = ref('')
const content = ref('')
const targetError = ref('')
const contentError = ref('')
const typeError = ref('')
const errorMessage = ref('')
const result = ref(null)
const testing = ref(false)
const { t, te } = useI18n()

const sendDisabled = computed(() => props.disabled || props.loading || testing.value)
const displayChannelType = computed(() => getChannelTypeLabel(props.channelType, t, te))

const targetLabel = computed(() => {
  switch (props.channelType) {
    case 'SMS':
      return '手机号'
    case 'EMAIL':
      return '邮箱地址'
    case 'PUSH':
      return '设备 Token'
    default:
      return '目标地址'
  }
})

const targetPlaceholder = computed(() => {
  switch (props.channelType) {
    case 'SMS':
      return '请输入手机号'
    case 'EMAIL':
      return '请输入邮箱地址'
    case 'PUSH':
      return '请输入设备 Token / FCM Token'
    default:
      return '请输入目标地址'
  }
})

const resetState = () => {
  target.value = ''
  content.value = ''
  targetError.value = ''
  contentError.value = ''
  typeError.value = ''
  errorMessage.value = ''
  result.value = null
}

watch(
  () => props.open,
  (value) => {
    if (value) {
      resetState()
    }
  }
)

const validate = () => {
  typeError.value = props.channelType ? '' : '请选择通道类型'
  targetError.value = target.value.trim() ? '' : `请填写${targetLabel.value}`
  contentError.value = content.value.trim() ? '' : '请输入测试内容'
  return !typeError.value && !targetError.value && !contentError.value
}

const submitTestSend = async () => {
  if (sendDisabled.value) return
  errorMessage.value = ''
  result.value = null
  if (!validate()) {
    message.warning('请先完善测试发送信息')
    return
  }
  testing.value = true
  try {
    const payload = {
      type: props.channelType,
      target: target.value.trim(),
      content: content.value.trim(),
      properties: props.properties || {}
    }
    const response = await testSendChannel(payload)
    result.value = response
    if (response?.success) {
      message.success('测试发送成功')
    } else {
      message.error(response?.errorMessage || '测试发送失败')
    }
  } catch (error) {
    errorMessage.value = error?.message || '测试发送失败'
  } finally {
    testing.value = false
  }
}

const handleCancel = () => {
  emit('cancel')
}

const resultMessage = computed(() => {
  if (!result.value) return ''
  if (result.value.success) {
    return result.value.messageId ? `测试发送成功，消息ID：${result.value.messageId}` : '测试发送成功'
  }
  return result.value.errorMessage || '测试发送失败'
})
</script>

<style scoped>
.test-modal {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.test-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.test-name {
  font-size: 12px;
}


.test-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
