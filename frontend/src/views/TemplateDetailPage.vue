<template>
  <section class="template-detail-layout">
    <!-- 顶部导航 -->
    <div class="detail-nav">
      <a-button @click="goBack">← 返回列表</a-button>
    </div>

    <a-spin :spinning="pageLoading">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <div class="info-card-header">
          <div>
            <h2 class="info-title">{{ template.name || '模板详情' }}</h2>
            <span class="info-code">{{ template.code }}</span>
          </div>
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

        <a-form
          ref="infoFormRef"
          :model="infoForm"
          :rules="infoRules"
          layout="vertical"
          :disabled="!infoEditing"
          style="margin-top: 16px"
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
          <a-form-item label="描述" name="description">
            <a-textarea
              v-model:value="infoForm.description"
              placeholder="请输入描述（可选）"
              :rows="2"
              :maxlength="1000"
              show-count
            />
          </a-form-item>
        </a-form>

        <div class="info-meta">
          <span>创建时间：{{ formatTimestamp(template.createdAt) }}</span>
          <span>更新时间：{{ formatTimestamp(template.updatedAt) }}</span>
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
              <a-alert
                v-if="langErrors[lang.code]"
                type="error"
                :message="langErrors[lang.code]"
                show-icon
                closable
                @close="langErrors[lang.code] = ''"
                style="margin-bottom: 12px"
              />

              <a-textarea
                v-model:value="langContents[lang.code]"
                :placeholder="`输入 ${lang.displayName} 的模板内容，支持变量占位符如 {{name}}...`"
                :rows="10"
                class="lang-textarea"
                @input="markDirty(lang.code)"
              />

              <div class="lang-tab-footer">
                <span v-if="dirtyLangs.has(lang.code)" class="unsaved-hint">有未保存的修改</span>
                <a-space>
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
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { formatTimestamp } from '../utils/time'
import {
  fetchTemplate,
  updateTemplate,
  fetchTemplateContents,
  saveLanguageContent,
  deleteLanguageContent
} from '../utils/templateApi'

const LANGUAGES = [
  { code: 'zh-Hans', displayName: '简体中文' },
  { code: 'zh-Hant', displayName: '繁体中文' },
  { code: 'en', displayName: 'English' },
  { code: 'fr', displayName: 'Français' },
  { code: 'de', displayName: 'Deutsch' }
]

const route = useRoute()
const router = useRouter()

const pageLoading = ref(false)
const template = ref({})
const activeTab = ref('zh-Hans')

const infoEditing = ref(false)
const infoSaving = ref(false)
const infoFormRef = ref(null)

const infoForm = reactive({ name: '', code: '', description: '' })
const infoRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const langContents = reactive({})
const originalContents = reactive({})
const dirtyLangs = ref(new Set())
const savingLangs = ref(new Set())
const deletingLangs = ref(new Set())
const langErrors = reactive({})

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
    infoForm.description = t.description || ''

    const contents = await fetchTemplateContents(id)
    LANGUAGES.forEach((lang) => {
      langContents[lang.code] = ''
      originalContents[lang.code] = ''
    })
    contents.forEach((lt) => {
      const code = lt.language?.code || lt.language
      if (code) {
        langContents[code] = lt.content || ''
        originalContents[code] = lt.content || ''
      }
    })
  } catch (e) {
    message.error(e?.message || '加载模板失败')
  } finally {
    pageLoading.value = false
  }
}

const startInfoEdit = () => {
  infoEditing.value = true
}

const cancelInfoEdit = () => {
  infoForm.name = template.value.name
  infoForm.code = template.value.code
  infoForm.description = template.value.description || ''
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
      description: infoForm.description
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
  langErrors[lang.code] = ''
  try {
    await saveLanguageContent(route.params.id, lang.code, langContents[lang.code])
    originalContents[lang.code] = langContents[lang.code]
    const newDirty = new Set(dirtyLangs.value)
    newDirty.delete(lang.code)
    dirtyLangs.value = newDirty
    message.success(`${lang.displayName} 内容已保存`)
  } catch (e) {
    langErrors[lang.code] = e?.message || '保存失败'
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
  background: var(--surface);
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 24px var(--shadow-soft);
  padding: 24px;
  color: var(--text-on-surface);
}

.info-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.info-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-on-surface);
}

.info-code {
  font-size: 13px;
  color: var(--text-muted);
  font-family: monospace;
}

.info-card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.info-meta {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid var(--border-subtle);
  padding-top: 12px;
}

.lang-card {
  background: var(--surface);
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 24px var(--shadow-soft);
  padding: 24px;
  color: var(--text-on-surface);
}

.lang-card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-on-surface);
}

.lang-tab-body {
  padding: 16px 0 0;
}

.lang-textarea {
  font-family: monospace;
  font-size: 13px;
  resize: vertical;
}

.lang-tab-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.unsaved-hint {
  font-size: 12px;
  color: var(--warning, #faad14);
}
</style>
