<template>
  <section class="template-detail-layout">
    <!-- 顶部导航 -->
    <div class="detail-nav">
      <a-button @click="goBack">← 返回列表</a-button>
    </div>

    <a-spin :spinning="pageLoading">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <!-- 只读展示 -->
        <a-descriptions
          v-if="!infoEditing"
          :column="2"
          bordered
          size="small"
          class="info-descriptions"
        >
          <a-descriptions-item label="模板名称">{{ template.name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="模板编码">
            <code class="desc-code">{{ template.code || '-' }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="通道类型">
            <a-tag v-if="template.channelType" color="blue">{{ channelTypeLabel(template.channelType) }}</a-tag>
            <span v-else class="desc-empty">未设置</span>
          </a-descriptions-item>
          <a-descriptions-item label="扩展属性" :span="2">
            <template v-if="attributeEntries.length">
              <a-table
                size="small"
                :pagination="false"
                :columns="attributeTableColumns"
                :data-source="attributeEntries"
                row-key="key"
                class="attr-readonly-table"
              />
            </template>
            <span v-else class="desc-empty">无</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTimestamp(template.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatTimestamp(template.updatedAt) }}</a-descriptions-item>
        </a-descriptions>

        <!-- 编辑表单 -->
        <a-form
          v-else
          ref="infoFormRef"
          :model="infoForm"
          :rules="infoRules"
          layout="vertical"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="模板名称" name="name">
                <a-input v-model:value="infoForm.name" placeholder="请输入模板名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="模板编码" name="code">
                <a-input v-model:value="infoForm.code" disabled />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="通道类型" name="channelType">
            <a-select
              v-model:value="infoForm.channelType"
              placeholder="选择通道类型"
              :options="channelTypeOptions"
              :disabled="channelTypeOptions.length === 1"
              allow-clear
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="扩展属性" name="attributes">
            <p class="form-hint">可选 如邮件或推送的主题等 键值均为文本</p>
            <div
              v-for="(row, idx) in infoAttributeRows"
              :key="idx"
              class="attr-row"
            >
              <a-input v-model:value="row.key" placeholder="键 如 subject" allow-clear />
              <a-input v-model:value="row.value" placeholder="值" allow-clear />
              <a-button type="text" danger @click="removeInfoAttributeRow(idx)">移除</a-button>
            </div>
            <a-button type="dashed" block class="attr-add" @click="addInfoAttributeRow">添加一行</a-button>
          </a-form-item>
          <div class="info-meta">
            <span>创建时间：{{ formatTimestamp(template.createdAt) }}</span>
            <span>更新时间：{{ formatTimestamp(template.updatedAt) }}</span>
          </div>
        </a-form>

        <div class="info-card-actions">
          <template v-if="!infoEditing">
            <a-button type="primary" ghost @click="startInfoEdit">编辑信息</a-button>
          </template>
          <template v-else>
            <a-button :loading="infoSaving" type="primary" @click="saveInfo">保存</a-button>
            <a-button @click="cancelInfoEdit">取消</a-button>
          </template>
        </div>
      </div>

      <!-- 多语言内容区域 -->
      <div class="lang-card">
        <h3 class="lang-card-title">多语言内容</h3>

        <a-tabs v-model:activeKey="activeTab" type="card">
          <a-tab-pane
            v-for="lang in LANGUAGES"
            :key="lang.code"
            :tab="getLangTabTitle(lang)"
          >
            <div class="lang-tab-body">
              <a-textarea
                v-model:value="langContents[lang.code]"
                :placeholder="contentPlaceholder(lang)"
                :rows="10"
                class="lang-textarea"
                @input="markDirty(lang.code)"
              />

              <div class="lang-tab-footer">
                <span v-if="dirtyLangs.has(lang.code)" class="unsaved-hint">有未保存的修改</span>
                <a-space class="lang-footer-actions">
                  <a-button
                    v-if="langContents[lang.code] && originalContents[lang.code]"
                    danger
                    size="small"
                    :loading="deletingLangs.has(lang.code)"
                    @click="confirmDeleteLang(lang)"
                  >删除此语言内容</a-button>
                  <a-button
                    type="primary"
                    :loading="savingLangs.has(lang.code)"
                    :disabled="!dirtyLangs.has(lang.code)"
                    @click="saveLangContent(lang)"
                  >保存</a-button>
                </a-space>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-spin>

    <!-- 删除语言内容确认弹窗 -->
    <a-modal
      v-model:open="deleteLangModal.open"
      title="确认删除语言内容"
      ok-type="danger"
      ok-text="删除"
      cancel-text="取消"
      :confirm-loading="deleteLangModal.loading"
      @ok="doDeleteLang"
      @cancel="deleteLangModal.open = false"
    >
      <p>
        确定要删除
        <strong>{{ deleteLangModal.lang?.displayName }}</strong>
        的模板内容吗？
      </p>
    </a-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { formatTimestamp } from '../utils/time'
import { fetchTemplate, updateTemplate, saveLanguageContent, deleteLanguageContent } from '../utils/templateApi'
import { CHANNEL_TYPE_VALUES, CHANNEL_TYPE_LABELS } from '../constants/channelTypes'

const channelTypeOptions = CHANNEL_TYPE_VALUES.map((v) => ({
  value: v,
  label: CHANNEL_TYPE_LABELS['zh-CN'][v] || v
}))

const channelTypeLabel = (value) => CHANNEL_TYPE_LABELS['zh-CN'][value] || value

const LANGUAGES = [
  { code: 'zh-CN', displayName: '简体中文' },
  { code: 'zh-TW', displayName: '繁体中文' },
  { code: 'en', displayName: 'English' },
  { code: 'fr', displayName: 'Français' },
  { code: 'de', displayName: 'Deutsch' }
]

const route = useRoute()
const router = useRouter()

const pageLoading = ref(false)
const template = ref({})
const activeTab = ref('zh-CN')

const infoEditing = ref(false)
const infoSaving = ref(false)
const infoFormRef = ref(null)

const infoForm = reactive({
  name: '',
  code: '',
  channelType: undefined
})

const infoAttributeRows = ref([{ key: '', value: '' }])

const attributeTableColumns = [
  { title: '键', dataIndex: 'key', key: 'key', ellipsis: true },
  { title: '值', dataIndex: 'value', key: 'value', ellipsis: true }
]

const attributeEntries = computed(() => {
  const a = template.value?.attributes
  if (!a || typeof a !== 'object') return []
  return Object.keys(a).map((k) => ({ key: k, value: a[k] ?? '' }))
})

const attributesToRows = (attrs) => {
  if (!attrs || typeof attrs !== 'object') {
    return [{ key: '', value: '' }]
  }
  const keys = Object.keys(attrs)
  if (keys.length === 0) {
    return [{ key: '', value: '' }]
  }
  return keys.map((k) => ({ key: k, value: attrs[k] ?? '' }))
}

const rowsToAttributesPayload = (rows) => {
  const out = {}
  for (const r of rows) {
    const k = (r.key || '').trim()
    if (k) {
      out[k] = r.value ?? ''
    }
  }
  return Object.keys(out).length > 0 ? out : null
}

const addInfoAttributeRow = () => {
  infoAttributeRows.value.push({ key: '', value: '' })
}

const removeInfoAttributeRow = (idx) => {
  if (infoAttributeRows.value.length <= 1) {
    infoAttributeRows.value = [{ key: '', value: '' }]
    return
  }
  infoAttributeRows.value.splice(idx, 1)
}

const infoRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const contentPlaceholder = (lang) =>
  `输入 ${lang.displayName} 的模板内容，支持变量占位符如 {{name}}`

const langContents = reactive({})
const originalContents = reactive({})
const dirtyLangs = ref(new Set())
const savingLangs = ref(new Set())
const deletingLangs = ref(new Set())

const deleteLangModal = reactive({ open: false, loading: false, lang: null })

const getLangTabTitle = (lang) => {
  const isDirty = dirtyLangs.value.has(lang.code)
  return isDirty ? `${lang.displayName} *` : lang.displayName
}

const goBack = () => router.push('/templates')

const loadTemplate = async () => {
  const id = route.params.id
  pageLoading.value = true
  try {
    const t = await fetchTemplate(id)
    template.value = t
    infoForm.name = t.name
    infoForm.code = t.code
    infoForm.channelType = t.channelType
    infoAttributeRows.value = attributesToRows(t.attributes)

    LANGUAGES.forEach((lang) => {
      langContents[lang.code] = ''
      originalContents[lang.code] = ''
    })
    const raw = t.templates
    if (raw && typeof raw === 'object') {
      LANGUAGES.forEach((lang) => {
        const entry = raw[lang.code]
        if (entry == null) return
        const text =
          typeof entry === 'string'
            ? entry
            : entry.content != null
              ? entry.content
              : ''
        langContents[lang.code] = text
        originalContents[lang.code] = text
      })
    }
  } catch (e) {
    message.error(e?.message || '加载模板失败')
  } finally {
    pageLoading.value = false
  }
}

const startInfoEdit = () => {
  infoAttributeRows.value = attributesToRows(template.value.attributes)
  infoEditing.value = true
}

const cancelInfoEdit = () => {
  infoForm.name = template.value.name
  infoForm.code = template.value.code
  infoForm.channelType = template.value.channelType
  infoAttributeRows.value = attributesToRows(template.value.attributes)
  infoEditing.value = false
  infoFormRef.value?.clearValidate()
}

const saveInfo = async () => {
  try {
    await infoFormRef.value.validate()
  } catch {
    return
  }
  infoSaving.value = true
  try {
    const updated = await updateTemplate(route.params.id, {
      name: infoForm.name,
      code: infoForm.code,
      channelType: infoForm.channelType,
      attributes: rowsToAttributesPayload(infoAttributeRows.value)
    })
    template.value = updated
    infoEditing.value = false
    message.success('模板信息已更新')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    infoSaving.value = false
  }
}

const markDirty = (code) => {
  const newSet = new Set(dirtyLangs.value)
  newSet.add(code)
  dirtyLangs.value = newSet
}

const saveLangContent = async (lang) => {
  const newSaving = new Set(savingLangs.value)
  newSaving.add(lang.code)
  savingLangs.value = newSaving
  try {
    await saveLanguageContent(route.params.id, lang.code, langContents[lang.code])
    originalContents[lang.code] = langContents[lang.code]
    const newDirty = new Set(dirtyLangs.value)
    newDirty.delete(lang.code)
    dirtyLangs.value = newDirty
    message.success(`${lang.displayName} 内容已保存`)
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    const newSaving2 = new Set(savingLangs.value)
    newSaving2.delete(lang.code)
    savingLangs.value = newSaving2
  }
}

const confirmDeleteLang = (lang) => {
  deleteLangModal.lang = lang
  deleteLangModal.open = true
}

const doDeleteLang = async () => {
  const lang = deleteLangModal.lang
  if (!lang) return
  deleteLangModal.loading = true
  const newDeleting = new Set(deletingLangs.value)
  newDeleting.add(lang.code)
  deletingLangs.value = newDeleting
  try {
    await deleteLanguageContent(route.params.id, lang.code)
    langContents[lang.code] = ''
    originalContents[lang.code] = ''
    const newDirty = new Set(dirtyLangs.value)
    newDirty.delete(lang.code)
    dirtyLangs.value = newDirty
    message.success(`${lang.displayName} 内容已删除`)
    deleteLangModal.open = false
  } catch (e) {
    message.error(e?.message || '删除失败')
  } finally {
    deleteLangModal.loading = false
    const newDeleting2 = new Set(deletingLangs.value)
    newDeleting2.delete(lang.code)
    deletingLangs.value = newDeleting2
  }
}

onMounted(loadTemplate)
</script>

<style scoped>
.template-detail-layout {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-nav {
  display: flex;
  align-items: center;
}

.info-card {
  border-radius: 12px;
  padding: 24px;
}

.desc-code {
  font-family: monospace;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
}

.desc-empty {
  font-size: 12px;
}

.info-card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  justify-content: flex-end;
  margin-top: 16px;
  width: 100%;
}

.info-meta {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  font-size: 12px;
  padding-top: 12px;
}

.lang-card {
  border-radius: 12px;
  padding: 24px;
}

.lang-card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}

.lang-card :deep(.ant-tabs-top > .ant-tabs-nav),
.lang-card :deep(.ant-tabs-top > div > .ant-tabs-nav) {
  margin-bottom: 0;
}

.lang-tab-body {
  padding: 0;
}

.lang-textarea {
  font-size: 13px;
  resize: vertical;
  line-height: 1.6;
}

.lang-tab-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.lang-footer-actions {
  margin-left: auto;
  flex-shrink: 0;
}

.unsaved-hint {
  font-size: 12px;
  color: #faad14;
}

.form-hint {
  margin: 0 0 8px;
  font-size: 12px;
  opacity: 0.65;
}

.attr-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.attr-row .ant-input {
  flex: 1;
  min-width: 0;
}

.attr-add {
  margin-top: 4px;
}

.attr-readonly-table {
  margin: 0;
}
</style>
