<template>
  <ChannelManagementLayout>
    <template #title>{{ pageTitle }}</template>
    <template #subtitle>配置通道类型、名称与属性</template>
    <template #actions>
      <a-space>
        <a-tooltip title="返回列表">
          <a-button @click="goBack">返回</a-button>
        </a-tooltip>
        <a-button :disabled="testDenied" @click="openTestModal">测试</a-button>
        <a-button type="primary" :loading="saving" :disabled="denied" @click="saveChannel">
          保存
        </a-button>
      </a-space>
    </template>

    <a-alert
      v-if="pageError"
      type="error"
      :message="pageError"
      show-icon
      closable
      @close="pageError = ''"
      style="margin-bottom: 16px"
    />

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
  </ChannelManagementLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import ChannelManagementLayout from '../components/channels/ChannelManagementLayout.vue'
import PropertyEditor from '../components/channels/PropertyEditor.vue'
import ChannelTestSendModal from '../components/channels/ChannelTestSendModal.vue'
import { createChannel, fetchChannel, fetchChannelTypes, fetchEmailProtocols, updateChannel } from '../utils/channelApi'
import { buildChannelTypeOptions, CHANNEL_TYPE_VALUES } from '../constants/channelTypes'
import {
  createObjectRow,
  createPropertyRow,
  propertiesToRows,
  rowsToProperties,
  validatePropertyRows
} from '../utils/propertyRows'
import { currentUser } from '../stores/auth'
import { CHANNEL_PERMISSIONS, hasPermission } from '../utils/permissions'

const route = useRoute()
const router = useRouter()
const { t, te } = useI18n()

const form = reactive({
  id: null,
  name: '',
  type: '',
  description: ''
})

const loading = ref(false)
const saving = ref(false)
const pageError = ref('')

const propertyRows = ref([createPropertyRow()])
const rowInvalidIds = ref(new Set())
const objectInvalidIds = ref({})
const objectErrors = ref({})
const nameError = ref('')
const typeError = ref('')
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

const imTypes = ref({ defaultType: 'DINGTALK', types: [] })
const imType = ref('')
const isImChannel = computed(() => form.type === 'IM')
const imTypeOptions = computed(() =>
  (imTypes.value.types || []).map((type) => ({
    value: type.type,
    label: type.label || type.type
  }))
)

const isEdit = computed(() => !!route.params.id)
const pageTitle = computed(() => (isEdit.value ? '编辑通道' : '新建通道'))

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
    // TODO: 将来可以从后端 API 获取 IM 类型列表
    imTypes.value = getFallbackImTypes()
  } catch (error) {
    imTypes.value = getFallbackImTypes()
  }
}

const loadChannel = async () => {
  if (!isEdit.value) return
  loading.value = true
  pageError.value = ''
  try {
    const data = await fetchChannel(route.params.id)
    form.id = data.id
    form.name = data.name || ''
    form.type = data.type || ''
    form.description = data.description || ''
    if (form.type === 'EMAIL') {
      const protocol = resolveProtocolValue(data.properties?.protocol)
      emailProtocol.value = protocol
      const schema = findEmailProtocolSchema(protocol)
      propertyRows.value = schema
        ? buildProtocolRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else if (form.type === 'IM') {
      const type = resolveImTypeValue(data.properties?.type)
      imType.value = type
      const schema = findImTypeSchema(type)
      propertyRows.value = schema
        ? buildImTypeRowsFromProperties(schema, data.properties)
        : propertiesToRows(data.properties)
    } else {
      propertyRows.value = propertiesToRows(data.properties)
    }
  } catch (error) {
    pageError.value = error?.message || '获取通道信息失败'
  } finally {
    loading.value = false
  }
}

const validateForm = () => {
  nameError.value = form.name.trim() ? '' : '通道名称不能为空'
  typeError.value = form.type ? '' : '请选择通道类型'

  const validation = validatePropertyRows(propertyRows.value)
  rowInvalidIds.value = validation.rowInvalidIds
  objectInvalidIds.value = validation.objectInvalidIds
  objectErrors.value = validation.objectErrors
  propertyError.value = validation.message

  const hasObjectErrors = Object.keys(validation.objectInvalidIds).length > 0
  return !nameError.value && !typeError.value && !propertyError.value && !hasObjectErrors
}

const buildProperties = () => {
  if (!isEmailChannel.value && !isImChannel.value) {
    return rowsToProperties(propertyRows.value)
  }
  if (isEmailChannel.value) {
    const protocol = resolveProtocolValue(emailProtocol.value)
    const schema = findEmailProtocolSchema(protocol)
    if (!schema) {
      return rowsToProperties(propertyRows.value)
    }
    const raw = rowsToProperties(propertyRows.value)
    const requiredFields = (schema.fields || []).filter((field) => field.required)
    const protocolPayload = {}
    requiredFields.forEach((field) => {
      if (raw[field.key] !== undefined) {
        protocolPayload[field.key] = raw[field.key]
      }
    })
    return {
      protocol,
      [schema.propertyKey]: protocolPayload
    }
  }
  // IM channel
  const type = resolveImTypeValue(imType.value)
  const schema = findImTypeSchema(type)
  if (!schema) {
    return rowsToProperties(propertyRows.value)
  }
  const raw = rowsToProperties(propertyRows.value)
  const requiredFields = (schema.fields || []).filter((field) => field.required)
  const typePayload = {}
  requiredFields.forEach((field) => {
    if (raw[field.key] !== undefined) {
      typePayload[field.key] = raw[field.key]
    }
  })
  return {
    type,
    [schema.propertyKey]: typePayload
  }
}

const saveChannel = async () => {
  if (denied.value) return
  if (!validateForm()) {
    message.warning('请先修复表单错误')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: form.type,
      description: form.description?.trim() || '',
      properties: buildProperties()
    }
    if (isEdit.value) {
      await updateChannel(form.id, payload)
      message.success('通道已更新')
    } else {
      await createChannel(payload)
      message.success('通道已创建')
    }
    router.push('/channels')
  } catch (error) {
    message.error(error?.message || '保存通道失败')
  } finally {
    saving.value = false
  }
}

const openTestModal = () => {
  if (testDenied.value) return
  typeError.value = form.type ? '' : '请选择通道类型'
  const validation = validatePropertyRows(propertyRows.value)
  rowInvalidIds.value = validation.rowInvalidIds
  objectInvalidIds.value = validation.objectInvalidIds
  objectErrors.value = validation.objectErrors
  propertyError.value = validation.message

  const hasObjectErrors = Object.keys(validation.objectInvalidIds).length > 0
  if (typeError.value || propertyError.value || hasObjectErrors) {
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
  await loadChannel()
  if (!isEdit.value && form.type === 'EMAIL') {
    initializeEmailProtocol()
  }
  if (!isEdit.value && form.type === 'IM') {
    initializeImType()
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

  const requiredFields = (schema.fields || []).filter((field) => field.required)
  propertyRows.value = mergeFlatRows(propertyRows.value, requiredFields, preferExisting)
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

const resolveImTypeValue = (value) => {
  const fallback = imTypes.value.defaultType || 'DINGTALK'
  if (!value) return fallback
  const normalized = String(value).trim()
  return normalized ? normalized.toUpperCase() : fallback
}

const buildImTypeRowsFromProperties = (schema, properties) => {
  const requiredFields = (schema.fields || []).filter((field) => field.required)
  return requiredFields.map((field) => {
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

const getFallbackImTypes = () => ({
  defaultType: 'DINGTALK',
  types: [
    {
      type: 'DINGTALK',
      label: '钉钉',
      propertyKey: 'dingtalk',
      fields: [
        { key: 'webhookUrl', label: 'Webhook URL', required: true, defaultValue: '' }
      ]
    },
    {
      type: 'WECHAT_WORK',
      label: '企业微信',
      propertyKey: 'wechatWork',
      fields: [
        { key: 'webhookUrl', label: 'Webhook URL', required: true, defaultValue: '' }
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
        { key: 'protocol', label: '传输协议', required: false, defaultValue: 'smtp' },
        { key: 'sslEnabled', label: '启用 SSL', required: false, defaultValue: 'true' },
        { key: 'from', label: '发件人', required: false, defaultValue: '' },
        { key: 'connectionTimeout', label: '连接超时(ms)', required: false, defaultValue: '5000' },
        { key: 'readTimeout', label: '读取超时(ms)', required: false, defaultValue: '10000' },
        { key: 'maxRetries', label: '最大重试次数', required: false, defaultValue: '3' },
        { key: 'retryDelay', label: '重试间隔(ms)', required: false, defaultValue: '1000' }
      ]
    },
    {
      protocol: 'HTTP_API',
      label: 'HTTP',
      propertyKey: 'httpApi',
      fields: [
        { key: 'baseUrl', label: 'API 基础地址', required: true, defaultValue: '' },
        { key: 'path', label: '发送路径', required: true, defaultValue: '' },
        { key: 'apiKeyHeader', label: '鉴权头', required: true, defaultValue: 'Authorization' },
        { key: 'apiKey', label: '鉴权令牌', required: true, defaultValue: '' },
        { key: 'from', label: '发件人', required: false, defaultValue: '' },
        { key: 'timeout', label: '超时(ms)', required: false, defaultValue: '5000' },
        { key: 'maxRetries', label: '最大重试次数', required: false, defaultValue: '3' },
        { key: 'retryDelay', label: '重试间隔(ms)', required: false, defaultValue: '1000' }
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
        imType.value = imTypes.value.defaultType || 'DINGTALK'
      }
      applyImTypeSchema(imType.value, { preferExisting: true })
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

</style>
