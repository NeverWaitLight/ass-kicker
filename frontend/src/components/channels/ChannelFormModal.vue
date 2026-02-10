<template>
  <a-modal
    :open="open"
    :title="mode === 'edit' ? '编辑渠道' : '新建渠道'"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form layout="vertical">
      <a-form-item label="渠道名称" required>
        <a-input v-model:value="form.name" placeholder="请输入渠道名称" />
      </a-form-item>
      <a-form-item label="渠道类型" required>
        <a-radio-group v-model:value="form.type" class="channel-type-group">
          <a-radio v-for="option in channelTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="form.description" rows="3" />
      </a-form-item>
      <a-form-item label="属性(JSON)" extra="可选，格式为 JSON 对象">
        <a-textarea v-model:value="propertiesText" rows="6" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { buildChannelTypeOptions, CHANNEL_TYPE_VALUES } from '../../constants/channelTypes'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  submitting: { type: Boolean, default: false },
  model: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['submit', 'cancel'])

const { t, te } = useI18n()
const channelTypeOptions = computed(() => buildChannelTypeOptions(CHANNEL_TYPE_VALUES, t, te))

const form = computed(() => props.model)
const propertiesText = ref('')

watch(
  () => props.model,
  (value) => {
    propertiesText.value = JSON.stringify(value?.properties || {}, null, 2)
  },
  { immediate: true, deep: true }
)

const handleSubmit = () => {
  if (!form.value.name) {
    message.warning('请填写渠道名称')
    return
  }
  if (!form.value.type || !CHANNEL_TYPE_VALUES.includes(form.value.type)) {
    message.warning('请选择有效的渠道类型')
    return
  }
  let parsed = {}
  if (propertiesText.value?.trim()) {
    try {
      parsed = JSON.parse(propertiesText.value)
    } catch (error) {
      message.error('属性字段需要为合法 JSON')
      return
    }
  }
  emit('submit', { ...form.value, properties: parsed })
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.channel-type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.channel-type-group :deep(.ant-radio-wrapper) {
  margin-inline-start: 0;
}
</style>
