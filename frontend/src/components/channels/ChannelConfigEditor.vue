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
              />
            </a-form-item>
            <a-form-item v-if="isEmailChannel" label="邮件协议">
              <a-select
                v-model:value="emailProtocol"
                placeholder="请选择邮件协议"
                :options="emailProtocolOptions"
                @change="handleProtocolChange"
              />
            </a-form-item>
            <a-form-item v-if="isImChannel" label="IM 类型">
              <a-select
                v-model:value="imType"
                placeholder="请选择 IM 类型"
                :options="imTypeOptions"
                @change="handleImTypeChange"
              />
            </a-form-item>
            <a-form-item v-if="isPushChannel" label="推送类型">
              <a-select
                v-model:value="pushType"
                placeholder="请选择推送类型"
                :options="pushTypeOptions"
                @change="handlePushTypeChange"
              />
            </a-form-item>
            <a-form-item v-if="isSmsChannel" label="短信服务">
              <a-select
                v-model:value="smsType"
                placeholder="请选择短信服务"
                :options="smsTypeOptions"
                @change="handleSmsTypeChange"
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
              label="收件人包含正则"
              :validate-status="includeRegexError ? 'error' : ''"
              :help="includeRegexError"
            >
              <a-input
                v-model:value="form.includeRecipientRegex"
                placeholder="可选，仅允许匹配该正则的收件人，优先于排除规则"
              />
            </a-form-item>
            <a-form-item
              label="收件人排除正则"
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

    <ChannelTestSendModal
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
import ChannelTestSendModal from './ChannelTestSendModal.vue'
import { createChannel, fetchChannel, fetchChannelTypes, fetchEmailProtocols, fetchImTypes, updateChannel } from '../../utils/channelApi'
import { buildChannelTypeOptions, CHANNEL_TYPE_VALUES } from '../../constants/channelTypes'
import {
  createObjectRow,
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
const emailProtocols = ref({ defaultProtocol: 'SMTP', protocols: [] })
const emailProtocol = ref('')
const isEmailChannel = computed(() => form.type === 'EMAIL')
const emailProtocolOptions = computed(() =>
  (emailProtocols.value.protocols || []).map((protocol) => ({
    value: protocol.protocol,
    label: protocol.label || protocol.protocol
  }))
)

const imTypes = ref({ defaultType: 'DINGTALK_WEBHOOK', types: [] })
const imType = ref('')
const isImChannel = computed(() => form.type === 'IM')
const imTypeOptions = computed(() =>
  (imTypes.value.types || []).map((type) => ({
    value: type.type,
    label: type.label || type.type
  }))
)

const pushTypes = ref({ defaultType: 'APNS', types: [] })
const pushType = ref('')
const isPushChannel = computed(() => form.type === 'PUSH')
const pushTypeOptions = computed(() =>
  (pushTypes.value.types || []).map((type) => ({
    value: type.type,
    label: type.label || type.type
  }))
)

const smsTypes = ref({ defaultType: 'ALIYUN_SMS', types: [] })
const smsType = ref('')
const isSmsChannel = computed(() => form.type === 'SMS')
const smsTypeOptions = computed(() =>
  (smsTypes.value.types || []).map((type) => ({
    value: type.type,
    label: type.label || type.type
  }))
)

const isEdit = computed(() => !!props.channelId)

const denied = computed(() => {
  const permission = isEdit.value ? CHANNEL_PERMISSIONS.edit : CHANNEL_PERMISSIONS.create
  return !hasPermission(currentUser.value, permission)
})

const testDenied = computed(() => denied.value || !form.type)
const baseSectionHint = '每一行可选择字符串或对象类型。'
const sectionHint = computed(() => {
  if (isEmailChannel.value) {
    const schema = findEmailProtocolSchema(emailProtocol.value)
    if (!schema) return baseSectionHint
    const requiredFields = (schema.fields || []).filter((field) => field.required)
    if (requiredFields.length === 0) return baseSectionHint
    const labels = requiredFields.map((field) => field.label || field.key).join('、')
    return `${baseSectionHint} ${schema.label || schema.protocol}必填：${labels}。`
  }
  if (isImChannel.value) {
    const schema = findImTypeSchema(imType.value)
    if (!schema) return baseSectionHint
    const requiredFields = (schema.fields || []).filter((field) => field.required)
    if (requiredFields.length === 0) return baseSectionHint
    const labels = requiredFields.map((field) => field.label || field.key).join('、')
    return `${baseSectionHint} ${schema.label || schema.type}必填：${labels}。`
  }
  if (isPushChannel.value) {
    const schema = findPushTypeSchema(pushType.value)
    if (!schema) return baseSectionHint
    const requiredFields = (schema.fields || []).filter((field) => field.required)
    if (requiredFields.length === 0) return baseSectionHint
    const labels = requiredFields.map((field) => field.label || field.key).join('、')
    return `${baseSectionHint} ${schema.label || schema.type}必填：${labels}。`
  }
  if (isSmsChannel.value) {
    const schema = findSmsTypeSchema(smsType.value)
    if (!schema) return baseSectionHint
    const requiredFields = (schema.fields || []).filter((field) => field.required)
    if (requiredFields.length === 0) return baseSectionHint
    const labels = requiredFields.map((field) => field.label || field.key).join('、')
    return `${baseSectionHint} ${schema.label || schema.type}必填：${labels}。`
  }
  return baseSectionHint
})

const loadTypes = async () => {
  try {
    const data = await fetchChannelTypes()
    channelTypes.value = data || []
  } catch (error) {
    channelTypes.value = CHANNEL_TYPE_VALUES
  }
}

const loadEmailProtocols = async () => {
  try {
    const data = await fetchEmailProtocols()
    emailProtocols.value = data || emailProtocols.value
  } catch (error) {
    emailProtocols.value = getFallbackEmailProtocols()
  }
}

const loadImTypes = async () => {
  try {
    const data = await fetchImTypes()
    imTypes.value = data || getFallbackImTypes()
  } catch (error) {
    imTypes.value = getFallbackImTypes()
  }
}

const loadPushTypes = () => {
  pushTypes.value = getFallbackPushTypes()
}

const loadSmsTypes = () => {
  smsTypes.value = getFallbackSmsTypes()
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
    if (form.type === 'EMAIL') {
      const protocol = resolveProtocolValue(data.properties?.protocol)
      emailProtocol.value = protocol
      const schema = findEmailProtocolSchema(protocol)
      propertyRows.value = schema
        ? buildProtocolRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else if (form.type === 'IM') {
      const type = resolveImTypeValue(data.provider ?? data.properties?.type)
      imType.value = type
      const schema = findImTypeSchema(type)
      propertyRows.value = schema
        ? buildImTypeRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else if (form.type === 'PUSH') {
      const type = resolvePushTypeValue(data.provider ?? data.properties?.type ?? data.properties?.protocol)
      pushType.value = type
      const schema = findPushTypeSchema(type)
      propertyRows.value = schema
        ? buildPushTypeRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else if (form.type === 'SMS') {
      const type = resolveSmsTypeValue(data.provider ?? data.properties?.type ?? data.properties?.protocol)
      smsType.value = type
      const schema = findSmsTypeSchema(type)
      propertyRows.value = schema
        ? buildSmsTypeRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else {
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
  await loadEmailProtocols()
  await loadImTypes()
  loadPushTypes()
  loadSmsTypes()
  await loadChannel()
  if (!isEdit.value && form.type === 'EMAIL') {
    initializeEmailProtocol()
  }
  if (!isEdit.value && form.type === 'IM') {
    initializeImType()
  }
  if (!isEdit.value && form.type === 'PUSH') {
    initializePushType()
  }
  if (!isEdit.value && form.type === 'SMS') {
    initializeSmsType()
  }
})

const testProperties = computed(() => buildProperties())

const handleProtocolChange = (value) => {
  if (!value) return
  emailProtocol.value = value
  applyEmailProtocolSchema(value, { preferExisting: true })
}

const handleImTypeChange = (value) => {
  if (!value) return
  imType.value = value
  applyImTypeSchema(value, { preferExisting: true })
}

const handlePushTypeChange = (value) => {
  if (!value) return
  pushType.value = value
  applyPushTypeSchema(value, { preferExisting: true })
}

const handleSmsTypeChange = (value) => {
  if (!value) return
  smsType.value = value
  applySmsTypeSchema(value, { preferExisting: true })
}

const initializeEmailProtocol = () => {
  const protocol = resolveProtocolValue(emailProtocol.value)
  emailProtocol.value = protocol
  applyEmailProtocolSchema(protocol, { preferExisting: true })
}

const initializeImType = () => {
  const type = resolveImTypeValue(imType.value)
  imType.value = type
  applyImTypeSchema(type, { preferExisting: true })
}

const initializePushType = () => {
  const type = resolvePushTypeValue(pushType.value)
  pushType.value = type
  applyPushTypeSchema(type, { preferExisting: true })
}

const initializeSmsType = () => {
  const type = resolveSmsTypeValue(smsType.value)
  smsType.value = type
  applySmsTypeSchema(type, { preferExisting: true })
}

const applyEmailProtocolSchema = (protocol, { preferExisting } = {}) => {
  if (!protocol || !isEmailChannel.value) return
  const schema = findEmailProtocolSchema(protocol)
  if (!schema) return

  const requiredFields = (schema.fields || []).filter((field) => field.required)
  propertyRows.value = mergeFlatRows(propertyRows.value, requiredFields, preferExisting)
}

const applyImTypeSchema = (type, { preferExisting } = {}) => {
  if (!type || !isImChannel.value) return
  const schema = findImTypeSchema(type)
  if (!schema) return

  const allFields = schema.fields || []
  propertyRows.value = mergeFlatRows(propertyRows.value, allFields, preferExisting)
}

const applyPushTypeSchema = (type, { preferExisting } = {}) => {
  if (!type || !isPushChannel.value) return
  const schema = findPushTypeSchema(type)
  if (!schema) return

  const allFields = schema.fields || []
  propertyRows.value = mergeFlatRows(propertyRows.value, allFields, preferExisting)
}

const applySmsTypeSchema = (type, { preferExisting } = {}) => {
  if (!type || !isSmsChannel.value) return
  const schema = findSmsTypeSchema(type)
  if (!schema) return

  const allFields = schema.fields || []
  propertyRows.value = mergeFlatRows(propertyRows.value, allFields, preferExisting)
}

const mergeObjectRows = (rows, fields, preferExisting, keepExtra = true) => {
  const existing = new Map()
  rows.forEach((row) => {
    existing.set(normalizeKey(row.key), row)
  })

  const ordered = []
  fields.forEach((field) => {
    const key = normalizeKey(field.key)
    const saved = existing.get(key)
    if (saved) {
      if (!preferExisting && field.defaultValue !== undefined && String(saved.value || '') === '') {
        saved.value = String(field.defaultValue ?? '')
      } else if (preferExisting && String(saved.value || '') === '' && field.defaultValue !== undefined) {
        saved.value = String(field.defaultValue ?? '')
      }
      ordered.push(saved)
      existing.delete(key)
    } else {
      ordered.push(createObjectRow({ key: field.key, value: String(field.defaultValue ?? '') }))
    }
  })

  if (keepExtra) {
    existing.forEach((row) => ordered.push(row))
  }
  return ordered
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

const findRow = (rows, key) => rows.find((row) => normalizeKey(row.key) === normalizeKey(key))

const resolveProtocolValue = (value) => {
  const fallback = emailProtocols.value.defaultProtocol || 'SMTP'
  if (!value) return fallback
  const normalized = String(value).trim()
  return normalized ? normalized.toUpperCase() : fallback
}

const buildProtocolRowsFromProperties = (schema, properties) => {
  const requiredFields = (schema.fields || []).filter((field) => field.required)
  return requiredFields.map((field) => {
    const value = getProtocolFieldValue(properties, schema.propertyKey, field.key, field.defaultValue)
    return createPropertyRow({ key: field.key, value })
  })
}

const getProtocolFieldValue = (properties, schemaKey, fieldKey, defaultValue) => {
  if (properties && typeof properties === 'object') {
    const nested = properties?.[schemaKey]
    if (nested && typeof nested === 'object' && nested[fieldKey] != null) {
      return String(nested[fieldKey])
    }
    if (properties[fieldKey] != null) {
      return String(properties[fieldKey])
    }
  }
  return String(defaultValue ?? '')
}

const findEmailProtocolSchema = (protocol) =>
  (emailProtocols.value.protocols || []).find((item) => item.protocol === protocol)

const findImTypeSchema = (type) =>
  (imTypes.value.types || []).find((item) => item.type === type)

const findPushTypeSchema = (type) =>
  (pushTypes.value.types || []).find((item) => item.type === type)

const findSmsTypeSchema = (type) =>
  (smsTypes.value.types || []).find((item) => item.type === type)

const resolveImTypeValue = (value) => {
  const fallback = imTypes.value.defaultType || 'DINGTALK_WEBHOOK'
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
  const fallback = pushTypes.value.defaultType || 'APNS'
  if (!value) return fallback
  const normalized = String(value).trim()
  return normalized ? normalized.toUpperCase() : fallback
}

const resolveSmsTypeValue = (value) => {
  const fallback = smsTypes.value.defaultType || 'ALIYUN_SMS'
  if (!value) return fallback
  const normalized = String(value).trim()
  const mapped = {
    ALIYUN: 'ALIYUN_SMS',
    AWS: 'AWS_SMS'
  }
  return mapped[normalized.toUpperCase()] || normalized.toUpperCase() || fallback
}

const buildImTypeRowsFromProperties = (schema, properties) => {
  const allFields = schema.fields || []
  return allFields.map((field) => {
    const value = getImTypeFieldValue(properties, schema.propertyKey, field.key, field.defaultValue)
    return createPropertyRow({ key: field.key, value })
  })
}

const getImTypeFieldValue = (properties, schemaKey, fieldKey, defaultValue) => {
  if (properties && typeof properties === 'object') {
    const nested = properties?.[schemaKey]
    if (nested && typeof nested === 'object' && nested[fieldKey] != null) {
      return String(nested[fieldKey])
    }
    if (properties[fieldKey] != null) {
      return String(properties[fieldKey])
    }
  }
  return String(defaultValue ?? '')
}

const buildPushTypeRowsFromProperties = (schema, properties) => {
  const allFields = schema.fields || []
  return allFields.map((field) => {
    const value = getPushTypeFieldValue(properties, schema.propertyKey, field.key, field.defaultValue)
    return createPropertyRow({ key: field.key, value })
  })
}

const getPushTypeFieldValue = (properties, schemaKey, fieldKey, defaultValue) => {
  if (properties && typeof properties === 'object') {
    const nested = properties?.[schemaKey]
    if (nested && typeof nested === 'object' && nested[fieldKey] != null) {
      return String(nested[fieldKey])
    }
    if (properties[fieldKey] != null) {
      return String(properties[fieldKey])
    }
  }
  return String(defaultValue ?? '')
}

const buildSmsTypeRowsFromProperties = (schema, properties) => {
  const allFields = schema.fields || []
  return allFields.map((field) => {
    const value = getSmsTypeFieldValue(properties, schema.propertyKey, field.key, field.defaultValue)
    return createPropertyRow({ key: field.key, value })
  })
}

const getSmsTypeFieldValue = (properties, schemaKey, fieldKey, defaultValue) => {
  if (properties && typeof properties === 'object') {
    const nested = properties?.[schemaKey]
    if (nested && typeof nested === 'object' && nested[fieldKey] != null) {
      return String(nested[fieldKey])
    }
    if (properties[fieldKey] != null) {
      return String(properties[fieldKey])
    }
  }
  return String(defaultValue ?? '')
}

const getFallbackImTypes = () => ({
  defaultType: 'DINGTALK_WEBHOOK',
  types: [
    {
      type: 'DINGTALK_WEBHOOK',
      label: '钉钉',
      propertyKey: 'dingtalkWebhook',
      fields: [
        { key: 'url', label: 'Webhook URL', required: true, defaultValue: '' }
      ]
    },
    {
      type: 'WECOM_WEBHOOK',
      label: '企业微信',
      propertyKey: 'wecomWebhook',
      fields: [
        { key: 'url', label: 'Webhook URL', required: true, defaultValue: '' }
      ]
    },
    {
      type: 'FEISHU_WEBHOOK',
      label: '飞书',
      propertyKey: 'feishuWebhook',
      fields: [
        { key: 'url', label: 'Webhook URL', required: true, defaultValue: '' }
      ]
    }
  ]
})

const getFallbackEmailProtocols = () => ({
  defaultProtocol: 'SMTP',
  protocols: [
    {
      protocol: 'SMTP',
      label: 'SMTP',
      propertyKey: 'smtp',
      fields: [
        { key: 'host', label: 'SMTP 主机', required: true, defaultValue: '' },
        { key: 'port', label: '端口', required: true, defaultValue: '465' },
        { key: 'username', label: '用户名', required: true, defaultValue: '' },
        { key: 'password', label: '密码', required: true, defaultValue: '' },
        { key: 'sslEnabled', label: '启用 SSL', required: false, defaultValue: 'true' },
        { key: 'starttls', label: '启用 STARTTLS', required: false, defaultValue: 'true' },
        { key: 'from', label: '发件人', required: false, defaultValue: '' },
        { key: 'connectionTimeout', label: '连接超时(ms)', required: false, defaultValue: '5000' },
        { key: 'readTimeout', label: '读取超时(ms)', required: false, defaultValue: '10000' }
      ]
    }
  ]
})

const getFallbackPushTypes = () => ({
  defaultType: 'APNS',
  types: [
    {
      type: 'APNS',
      label: '苹果 APNs',
      propertyKey: 'apns',
      fields: [
        { key: 'teamId', label: 'Team ID', required: true, defaultValue: '' },
        { key: 'keyId', label: 'Key ID', required: true, defaultValue: '' },
        { key: 'url', label: 'APNs URL', required: true, defaultValue: 'https://api.push.apple.com/3/device' },
        { key: 'bundleIdTopic', label: 'Bundle ID Topic', required: true, defaultValue: '' },
        { key: 'privateKeyPem', label: 'P8 私钥内容', required: true, defaultValue: '' },
        { key: 'apnsId', label: 'APNs ID', required: false, defaultValue: '' }
      ]
    },
    {
      type: 'FCM',
      label: '谷歌 FCM',
      propertyKey: 'fcm',
      fields: [
        { key: 'url', label: 'FCM Base URL', required: true, defaultValue: 'https://fcm.googleapis.com' },
        { key: 'projectId', label: 'Project ID', required: true, defaultValue: '' },
        { key: 'accessToken', label: 'Access Token', required: true, defaultValue: '' }
      ]
    }
  ]
})

const getFallbackSmsTypes = () => ({
  defaultType: 'ALIYUN_SMS',
  types: [
    {
      type: 'ALIYUN_SMS',
      label: '阿里云短信',
      propertyKey: 'aliyunSms',
      fields: [
        { key: 'accessKeyId', label: 'AccessKey ID', required: true, defaultValue: '' },
        { key: 'accessKeySecret', label: 'AccessKey Secret', required: true, defaultValue: '' },
        { key: 'signName', label: '签名名称', required: true, defaultValue: '' },
        { key: 'templateCode', label: '模板编码', required: true, defaultValue: '' },
        { key: 'regionId', label: 'Region ID', required: false, defaultValue: 'cn-hangzhou' },
        { key: 'endpoint', label: 'Endpoint', required: true, defaultValue: 'dysmsapi.aliyuncs.com' }
      ]
    },
    {
      type: 'AWS_SMS',
      label: 'AWS SNS 短信',
      propertyKey: 'awsSms',
      fields: [
        { key: 'accessKeyId', label: 'AccessKey ID', required: true, defaultValue: '' },
        { key: 'secretAccessKey', label: 'Secret Access Key', required: true, defaultValue: '' },
        { key: 'region', label: 'Region', required: true, defaultValue: 'ap-southeast-1' },
        { key: 'sessionToken', label: 'Session Token', required: false, defaultValue: '' },
        { key: 'endpoint', label: 'Endpoint', required: false, defaultValue: '' }
      ]
    }
  ]
})

watch(
  () => form.type,
  (value) => {
    if (value === 'EMAIL') {
      if (!emailProtocol.value) {
        emailProtocol.value = emailProtocols.value.defaultProtocol || 'SMTP'
      }
      applyEmailProtocolSchema(emailProtocol.value, { preferExisting: true })
    } else if (value === 'IM') {
      if (!imType.value) {
        imType.value = imTypes.value.defaultType || 'DINGTALK_WEBHOOK'
      }
      applyImTypeSchema(imType.value, { preferExisting: true })
    } else if (value === 'PUSH') {
      if (!pushType.value) {
        pushType.value = pushTypes.value.defaultType || 'APNS'
      }
      applyPushTypeSchema(pushType.value, { preferExisting: true })
    } else if (value === 'SMS') {
      if (!smsType.value) {
        smsType.value = smsTypes.value.defaultType || 'ALIYUN_SMS'
      }
      applySmsTypeSchema(smsType.value, { preferExisting: true })
    }
  }
)

watch(
  () => emailProtocols.value,
  () => {
    if (isEmailChannel.value) {
      initializeEmailProtocol()
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
