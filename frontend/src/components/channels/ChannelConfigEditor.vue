<template>
  <div class="channel-config-editor">
    <a-result
      v-if="denied"
      status="403"
      title="暂无权限"
      sub-title="请联系管理员开通通道配置权限。"
    >
      <template #extra>
        <a-tooltip title="返回列表">
          <a-button type="primary" @click="goBack">返回</a-button>
        </a-tooltip>
      </template>
    </a-result>

    <template v-else>
      <a-spin :spinning="loading">
        <div class="config-section config-section--flat">
          <a-form layout="vertical" class="config-form">
            <a-form-item label="通道类型" :validate-status="typeError ? 'error' : ''" :help="typeError">
              <a-select
                v-model:value="form.type"
                placeholder="请选择通道类型"
                allow-clear
                :options="typeOptions"
                :disabled="typeOptions.length === 1"
              />
            </a-form-item>
            <a-form-item v-if="providerTypeOptions.length > 0" label="服务商">
              <a-select
                v-model:value="providerType"
                placeholder="请选择服务商"
                :options="providerTypeOptions"
                :disabled="providerTypeOptions.length === 1"
                @change="handleProviderTypeChange"
              />
            </a-form-item>
            <a-form-item label="通道名称" :validate-status="nameError ? 'error' : ''" :help="nameError">
              <a-input v-model:value="form.name" placeholder="请输入通道名称" />
            </a-form-item>
          </a-form>

          <div class="property-block">
            <h3>属性配置</h3>
            <p class="section-hint">{{ sectionHint }}</p>
            <PropertyEditor
              v-model:rows="propertyRows"
              :row-invalid-ids="rowInvalidIds"
              :object-invalid-ids="objectInvalidIds"
              :object-errors="objectErrors"
              :error="propertyError"
            />
          </div>

          <a-form layout="vertical" class="config-form config-form--description">
            <a-form-item
              label="优先发送"
              :validate-status="includeRegexError ? 'error' : ''"
              :help="includeRegexError"
            >
              <a-input
                v-model:value="form.includeRecipientRegex"
                placeholder="可选，仅允许匹配该正则的收件人，优先于排除规则"
              />
            </a-form-item>
            <a-form-item
              label="拒绝发送"
              :validate-status="excludeRegexError ? 'error' : ''"
              :help="excludeRegexError"
            >
              <a-input
                v-model:value="form.excludeRecipientRegex"
                placeholder="可选，未配置包含规则时生效，匹配则拒绝"
              />
            </a-form-item>
            <a-form-item label="描述">
              <a-textarea v-model:value="form.description" rows="3" />
            </a-form-item>
          </a-form>
        </div>
      </a-spin>
    </template>

    <ChannelTestModal
      :open="testModalOpen"
      :channel-type="form.type"
      :channel-name="form.name"
      :properties="testProperties"
      :disabled="testDenied"
      @cancel="closeTestModal"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PropertyEditor from './PropertyEditor.vue'
import ChannelTestModal from './ChannelTestModal.vue'
import {
  createChannel,
  fetchChannel,
  fetchChannelTypes,
  fetchProvidersByChannelType,
  fetchProviderProperties,
  updateChannel
} from '../../utils/channelApi'
import { buildChannelTypeOptions, CHANNEL_TYPE_VALUES } from '../../constants/channelTypes'
import {
  createPropertyRow,
  propertiesToRows,
  rowsToProperties,
  validatePropertyRows
} from '../../utils/propertyRows'
import { currentUser } from '../../stores/auth'
import { CHANNEL_PERMISSIONS, hasPermission } from '../../utils/permissions'

const props = defineProps({
  channelId: { type: String, default: null },
  embedded: { type: Boolean, default: false }
})

const emit = defineEmits(['saved'])

const router = useRouter()
const { t, te } = useI18n()

const form = reactive({
  id: null,
  key: '',
  name: '',
  type: '',
  description: '',
  includeRecipientRegex: '',
  excludeRecipientRegex: ''
})

const loading = ref(false)
const saving = ref(false)

const propertyRows = ref([createPropertyRow()])
const providerSchemaFields = ref([])
const rowInvalidIds = ref(new Set())
const objectInvalidIds = ref({})
const objectErrors = ref({})
const nameError = ref('')
const typeError = ref('')
const includeRegexError = ref('')
const excludeRegexError = ref('')
const propertyError = ref('')
const testModalOpen = ref(false)

const channelTypes = ref([])
const typeOptions = computed(() => buildChannelTypeOptions(channelTypes.value, t, te))
const providerOptions = ref([])
const emailProtocol = ref('')
const isEmailChannel = computed(() => form.type === 'EMAIL')
const imType = ref('')
const isImChannel = computed(() => form.type === 'IM')
const pushType = ref('')
const isPushChannel = computed(() => form.type === 'PUSH')
const smsType = ref('')
const isSmsChannel = computed(() => form.type === 'SMS')
const providerType = computed({
  get: () => {
    if (isEmailChannel.value) return emailProtocol.value
    if (isImChannel.value) return imType.value
    if (isPushChannel.value) return pushType.value
    if (isSmsChannel.value) return smsType.value
    return ''
  },
  set: (value) => {
    if (isEmailChannel.value) emailProtocol.value = value
    else if (isImChannel.value) imType.value = value
    else if (isPushChannel.value) pushType.value = value
    else if (isSmsChannel.value) smsType.value = value
  }
})
const providerTypeOptions = computed(() => {
  return providerOptions.value
})

const isEdit = computed(() => !!props.channelId)

const denied = computed(() => {
  const permission = isEdit.value ? CHANNEL_PERMISSIONS.edit : CHANNEL_PERMISSIONS.create
  return !hasPermission(currentUser.value, permission)
})

const testDenied = computed(() => denied.value || !form.type)
const baseSectionHint = '每一行可选择字符串或对象类型。'
const sectionHint = computed(() => {
  const requiredFields = (providerSchemaFields.value || []).filter((field) => field.required)
  if (requiredFields.length === 0) return baseSectionHint
  const labels = requiredFields.map((field) => field.key).join('、')
  const providerLabel = providerTypeOptions.value.find((item) => item.value === providerType.value)?.label || providerType.value
  if (!providerLabel) return `${baseSectionHint} 必填：${labels}。`
  return `${baseSectionHint} ${providerLabel}必填：${labels}。`
})

const loadProviderSchemaFields = async (selectedProviderType) => {
  if (!selectedProviderType) {
    providerSchemaFields.value = []
    return []
  }
  try {
    const schema = await fetchProviderProperties(selectedProviderType)
    const fields = Array.isArray(schema?.properties) ? schema.properties : []
    providerSchemaFields.value = fields
    return fields
  } catch (error) {
    providerSchemaFields.value = []
    return []
  }
}

const loadProviderOptionsByChannelType = async (channelType) => {
  if (!channelType) {
    providerOptions.value = []
    return []
  }
  try {
    const options = await fetchProvidersByChannelType(channelType)
    providerOptions.value = Array.isArray(options) ? options : []
    return providerOptions.value
  } catch (error) {
    providerOptions.value = []
    return []
  }
}

const loadTypes = async () => {
  try {
    const data = await fetchChannelTypes()
    channelTypes.value = data || []
  } catch (error) {
    channelTypes.value = CHANNEL_TYPE_VALUES
  }
}

const loadChannel = async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await fetchChannel(props.channelId)
    form.id = data.id
    form.key = data.key || ''
    form.name = data.name || ''
    form.type = data.type || ''
    form.description = data.description || ''
    form.includeRecipientRegex = data.priorityAddressRegex || data.includeRecipientRegex || ''
    form.excludeRecipientRegex = data.excludeAddressRegex || data.excludeRecipientRegex || ''
    await loadProviderOptionsByChannelType(form.type)
    if (form.type === 'EMAIL') {
      const protocol = resolveProtocolValue(data.providerType ?? data.provider ?? data.properties?.protocol)
      emailProtocol.value = protocol
      await loadProviderSchemaFields(protocol)
      propertyRows.value = propertiesToRows(data.properties)
    } else if (form.type === 'IM') {
      const type = resolveImTypeValue(data.providerType ?? data.provider ?? data.properties?.type)
      imType.value = type
      await loadProviderSchemaFields(type)
      propertyRows.value = propertiesToRows(data.properties)
    } else if (form.type === 'PUSH') {
      const type = resolvePushTypeValue(data.providerType ?? data.provider ?? data.properties?.type ?? data.properties?.protocol)
      pushType.value = type
      await loadProviderSchemaFields(type)
      propertyRows.value = propertiesToRows(data.properties)
    } else if (form.type === 'SMS') {
      const type = resolveSmsTypeValue(data.providerType ?? data.provider ?? data.properties?.type ?? data.properties?.protocol)
      smsType.value = type
      await loadProviderSchemaFields(type)
      propertyRows.value = propertiesToRows(data.properties)
    } else {
      providerSchemaFields.value = []
      propertyRows.value = propertiesToRows(data.properties)
    }
  } catch (error) {
    message.error(error?.message || '获取通道信息失败')
  } finally {
    loading.value = false
  }
}

const validateRegexField = (value) => {
  const trimmed = (value || '').trim()
  if (!trimmed) return ''
  try {
    new RegExp(trimmed)
    return ''
  } catch {
    return '正则语法无效'
  }
}

const validateForm = () => {
  nameError.value = form.name.trim() ? '' : '通道名称不能为空'
  typeError.value = form.type ? '' : '请选择通道类型'
  includeRegexError.value = validateRegexField(form.includeRecipientRegex)
  excludeRegexError.value = validateRegexField(form.excludeRecipientRegex)

  const validation = validatePropertyRows(propertyRows.value)
  rowInvalidIds.value = validation.rowInvalidIds
  objectInvalidIds.value = validation.objectInvalidIds
  objectErrors.value = validation.objectErrors
  propertyError.value = validation.message

  const hasObjectErrors = Object.keys(validation.objectInvalidIds).length > 0
  return (
    !nameError.value &&
    !typeError.value &&
    !includeRegexError.value &&
    !excludeRegexError.value &&
    !propertyError.value &&
    !hasObjectErrors
  )
}

const buildProperties = () => rowsToProperties(propertyRows.value)

const resolveProvider = () => {
  if (isEmailChannel.value) {
    return resolveProtocolValue(emailProtocol.value)
  }
  if (isImChannel.value) {
    return resolveImTypeValue(imType.value)
  }
  if (isPushChannel.value) {
    return resolvePushTypeValue(pushType.value)
  }
  if (isSmsChannel.value) {
    return resolveSmsTypeValue(smsType.value)
  }
  return ''
}

const saveChannel = async () => {
  if (denied.value) return false
  if (!validateForm()) {
    message.warning('请先修复表单错误')
    return false
  }
  saving.value = true
  try {
    const payload = {
      key: (form.key || form.name).trim(),
      name: form.name.trim(),
      type: form.type,
      provider: resolveProvider(),
      providerType: resolveProvider(),
      description: form.description?.trim() || '',
      priorityAddressRegex: form.includeRecipientRegex?.trim() || '',
      excludeAddressRegex: form.excludeRecipientRegex?.trim() || '',
      properties: buildProperties()
    }
    if (isEdit.value) {
      payload.id = form.id
      await updateChannel(form.id, payload)
      message.success('通道已更新')
    } else {
      await createChannel(payload)
      message.success('通道已创建')
    }
    if (props.embedded) {
      emit('saved')
    } else {
      router.push('/channels')
    }
    return true
  } catch (error) {
    message.error(error?.message || '保存通道失败')
    return false
  } finally {
    saving.value = false
  }
}

const openTestModal = () => {
  if (testDenied.value) return
  typeError.value = form.type ? '' : '请选择通道类型'
  includeRegexError.value = validateRegexField(form.includeRecipientRegex)
  excludeRegexError.value = validateRegexField(form.excludeRecipientRegex)
  const validation = validatePropertyRows(propertyRows.value)
  rowInvalidIds.value = validation.rowInvalidIds
  objectInvalidIds.value = validation.objectInvalidIds
  objectErrors.value = validation.objectErrors
  propertyError.value = validation.message

  const hasObjectErrors = Object.keys(validation.objectInvalidIds).length > 0
  if (
    typeError.value ||
    includeRegexError.value ||
    excludeRegexError.value ||
    propertyError.value ||
    hasObjectErrors
  ) {
    message.warning('请先完善通道配置再进行测试发送')
    return
  }
  testModalOpen.value = true
}

const closeTestModal = () => {
  testModalOpen.value = false
}

const goBack = () => {
  router.push('/channels')
}

onMounted(async () => {
  await loadTypes()
  await loadChannel()
  if (!isEdit.value && form.type) {
    await loadProviderOptionsByChannelType(form.type)
  }
})

const testProperties = computed(() => buildProperties())

const handleProtocolChange = async (value) => {
  if (!value) return
  emailProtocol.value = value
  await applyEmailProtocolSchema(value, { preferExisting: true })
}

const handleImTypeChange = async (value) => {
  if (!value) return
  imType.value = value
  await applyImTypeSchema(value, { preferExisting: true })
}

const handlePushTypeChange = async (value) => {
  if (!value) return
  pushType.value = value
  await applyPushTypeSchema(value, { preferExisting: true })
}

const handleSmsTypeChange = async (value) => {
  if (!value) return
  smsType.value = value
  await applySmsTypeSchema(value, { preferExisting: true })
}

const handleProviderTypeChange = async (value) => {
  if (!value) return
  if (isEmailChannel.value) {
    await handleProtocolChange(value)
  } else if (isImChannel.value) {
    await handleImTypeChange(value)
  } else if (isPushChannel.value) {
    await handlePushTypeChange(value)
  } else if (isSmsChannel.value) {
    await handleSmsTypeChange(value)
  }
}

const applyEmailProtocolSchema = async (protocol, { preferExisting } = {}) => {
  if (!protocol || !isEmailChannel.value) return
  const fields = await loadProviderSchemaFields(protocol)
  propertyRows.value = mergeFlatRows(propertyRows.value, fields, preferExisting)
}

const applyImTypeSchema = async (type, { preferExisting } = {}) => {
  if (!type || !isImChannel.value) return
  const fields = await loadProviderSchemaFields(type)
  propertyRows.value = mergeFlatRows(propertyRows.value, fields, preferExisting)
}

const applyPushTypeSchema = async (type, { preferExisting } = {}) => {
  if (!type || !isPushChannel.value) return
  const fields = await loadProviderSchemaFields(type)
  propertyRows.value = mergeFlatRows(propertyRows.value, fields, preferExisting)
}

const applySmsTypeSchema = async (type, { preferExisting } = {}) => {
  if (!type || !isSmsChannel.value) return
  const fields = await loadProviderSchemaFields(type)
  propertyRows.value = mergeFlatRows(propertyRows.value, fields, preferExisting)
}

const mergeFlatRows = (rows, fields, preferExisting) => {
  const existing = new Map()
  rows.forEach((row) => {
    existing.set(normalizeKey(row.key), row)
  })
  return fields.map((field) => {
    const key = normalizeKey(field.key)
    const saved = existing.get(key)
    if (saved) {
      if (preferExisting && String(saved.value || '') === '' && field.defaultValue !== undefined) {
        saved.value = String(field.defaultValue ?? '')
      }
      return saved
    }
    return createPropertyRow({ key: field.key, value: String(field.defaultValue ?? '') })
  })
}

const normalizeKey = (value) => (value == null ? '' : String(value).trim().toLowerCase())

const resolveProtocolValue = (value) => {
  const fallback = providerTypeOptions.value[0]?.value || 'SMTP'
  if (!value) return fallback
  const normalized = String(value).trim()
  return normalized ? normalized.toUpperCase() : fallback
}


const resolveImTypeValue = (value) => {
  const fallback = providerTypeOptions.value[0]?.value || 'DINGTALK_WEBHOOK'
  if (!value) return fallback
  const normalized = String(value).trim()
  const mapped = {
    DINGTALK: 'DINGTALK_WEBHOOK',
    WECOM: 'WECOM_WEBHOOK',
    FEISHU: 'FEISHU_WEBHOOK'
  }
  return mapped[normalized.toUpperCase()] || normalized.toUpperCase() || fallback
}

const resolvePushTypeValue = (value) => {
  const fallback = providerTypeOptions.value[0]?.value || 'APNS'
  if (!value) return fallback
  const normalized = String(value).trim()
  return normalized ? normalized.toUpperCase() : fallback
}

const resolveSmsTypeValue = (value) => {
  const fallback = providerTypeOptions.value[0]?.value || 'ALIYUN_SMS'
  if (!value) return fallback
  const normalized = String(value).trim()
  const mapped = {
    ALIYUN: 'ALIYUN_SMS',
    AWS: 'AWS_SMS'
  }
  return mapped[normalized.toUpperCase()] || normalized.toUpperCase() || fallback
}


watch(
  () => form.type,
  async (value) => {
    await loadProviderOptionsByChannelType(value)
    if (value === 'EMAIL') {
      if (!emailProtocol.value) {
        emailProtocol.value = resolveProtocolValue('')
      }
      await applyEmailProtocolSchema(resolveProtocolValue(emailProtocol.value), { preferExisting: true })
    } else if (value === 'IM') {
      if (!imType.value) {
        imType.value = resolveImTypeValue('')
      }
      await applyImTypeSchema(resolveImTypeValue(imType.value), { preferExisting: true })
    } else if (value === 'PUSH') {
      if (!pushType.value) {
        pushType.value = resolvePushTypeValue('')
      }
      await applyPushTypeSchema(resolvePushTypeValue(pushType.value), { preferExisting: true })
    } else if (value === 'SMS') {
      if (!smsType.value) {
        smsType.value = resolveSmsTypeValue('')
      }
      await applySmsTypeSchema(resolveSmsTypeValue(smsType.value), { preferExisting: true })
    } else {
      providerSchemaFields.value = []
      providerOptions.value = []
    }
  }
)

defineExpose({
  saveChannel,
  openTestModal,
  saving,
  denied,
  testDenied
})
</script>

<style scoped>
.config-form {
  background: transparent;
  padding: 0;
  border-radius: 0;
  border: none;
  box-shadow: none;
}

.config-section {
  margin-top: 24px;
  padding: 0;
  background: transparent;
  border-radius: 0;
  border: none;
  box-shadow: none;
}

.config-section--flat {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.property-block {
  margin-top: 8px;
}

.config-form--description {
  margin-top: 8px;
}

.config-section h3 {
  margin: 0 0 4px;
}

.section-hint {
  margin: 0 0 12px;
  font-size: 12px;
}

.channel-config-editor__toolbar {
  margin-bottom: 12px;
}

</style>
