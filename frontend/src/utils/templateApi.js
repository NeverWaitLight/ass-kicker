import { apiFetch } from './api'

export const fetchTemplates = async () => {
  const response = await apiFetch('/api/templates')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchTemplate = async (id) => {
  const response = await apiFetch(`/api/templates/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const createTemplate = async (payload) => {
  const response = await apiFetch('/api/templates', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const updateTemplate = async (id, payload) => {
  const response = await apiFetch(`/api/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const deleteTemplate = async (id) => {
  const response = await apiFetch(`/api/templates/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}

export const fetchTemplateContents = async (templateId) => {
  const response = await apiFetch(`/api/templates/${templateId}/contents`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const saveLanguageContent = async (templateId, langCode, content) => {
  const response = await apiFetch(`/api/templates/${templateId}/languages/${langCode}`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: content
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const deleteLanguageContent = async (templateId, langCode) => {
  const contentsResponse = await apiFetch(`/api/templates/${templateId}/languages/${langCode}`)
  if (!contentsResponse.ok) {
    return true
  }
  const lt = await contentsResponse.json()
  if (!lt || !lt.id) return true
  const response = await apiFetch(`/api/language-templates/${lt.id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}
