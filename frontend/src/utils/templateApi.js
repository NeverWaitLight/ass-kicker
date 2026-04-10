import { unwrapData } from './apiPayload'
import { apiFetch } from './v1'

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
  const response = await apiFetch(`/v1/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
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
  const response = await apiFetch(`/v1/templates/${templateId}/contents`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const saveLanguageContent = async (templateId, langCode, content) => {
  const response = await apiFetch(`/v1/templates/${templateId}/languages/${langCode}`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: content
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const deleteLanguageContent = async (templateId, langCode) => {
  const contentsResponse = await apiFetch(`/v1/templates/${templateId}/languages/${langCode}`)
  if (!contentsResponse.ok) {
    return true
  }
  const raw = await contentsResponse.json()
  const lt = unwrapData(raw)
  if (!lt || !lt.id) return true
  const response = await apiFetch(`/v1/language-templates/${lt.id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}
