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
          <a-button type="primary" :loading="testing" :disabled="blocked" @click="submitTest">
            发送
          </a-button>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { testChannel } from '../../utils/channelApi'
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
const testing = ref(false)
const { t, te } = useI18n()

const blocked = computed(() => props.disabled || props.loading || testing.value)
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

const submitTest = async () => {
  if (blocked.value) return
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
    const response = await testChannel(payload)
    if (response?.success) {
      message.success('测试发送成功')
    } else {
      message.error(response?.errorMessage || '测试发送失败')
    }
  } catch (error) {
    message.error(error?.message || '测试发送失败')
  } finally {
    testing.value = false
  }
}

const handleCancel = () => {
  emit('cancel')
}
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
