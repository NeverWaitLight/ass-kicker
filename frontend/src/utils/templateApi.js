import { unwrapData } from './apiPayload'
import { apiFetch } from './v1'

function normalizeTemplatesObject(templates) {
  if (!templates || typeof templates !== 'object') return {}
  return JSON.parse(JSON.stringify(templates))
}

export const fetchTemplates = async () => {
  const response = await apiFetch('/v1/templates')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const fetchTemplate = async (id) => {
  const response = await apiFetch(`/v1/templates/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const createTemplate = async (payload) => {
  const response = await apiFetch('/v1/templates', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const updateTemplate = async (id, payload) => {
  const response = await apiFetch('/v1/templates', {
    method: 'PUT',
    body: JSON.stringify({ id, ...payload })
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const deleteTemplate = async (id) => {
  const response = await apiFetch(`/v1/templates/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}

export const fetchTemplateContents = async (templateId) => {
  const t = await fetchTemplate(templateId)
  return templatesVoToLegacyContentsList(t.templates)
}

function templatesVoToLegacyContentsList(templates) {
  if (!templates || typeof templates !== 'object') return []
  return Object.keys(templates).map((code) => {
    const entry = templates[code]
    const content =
      typeof entry === 'string'
        ? entry
        : entry && entry.content != null
          ? entry.content
          : ''
    return { language: { code }, content }
  })
}

export const saveLanguageContent = async (templateId, langCode, content) => {
  const t = await fetchTemplate(templateId)
  const next = normalizeTemplatesObject(t.templates)
  const prev = next[langCode]
  next[langCode] = {
    ...(typeof prev === 'object' && prev !== null && !Array.isArray(prev) ? prev : {}),
    content: content ?? ''
  }
  return updateTemplate(templateId, {
    code: t.code,
    channelType: t.channelType,
    templates: next
  })
}

export const deleteLanguageContent = async (templateId, langCode) => {
  const t = await fetchTemplate(templateId)
  const next = normalizeTemplatesObject(t.templates)
  if (next && typeof next === 'object') {
    delete next[langCode]
  }
  await updateTemplate(templateId, {
    code: t.code,
    channelType: t.channelType,
    templates: next
  })
  return true
}
