import { unwrapData, unwrapPage } from './apiPayload'
import { apiFetch } from './v1'

export const fetchGlobalVariablesPage = async (params = {}) => {
  const { page = 1, size = 10, keyword = '' } = params
  const search = new URLSearchParams({
    page: String(page),
    size: String(size),
    keyword: keyword ?? ''
  })
  const response = await apiFetch(`/v1/global-variables?${search.toString()}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapPage(json)
}

export const fetchGlobalVariable = async (id) => {
  const response = await apiFetch(`/v1/global-variables/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const createGlobalVariable = async (payload) => {
  const response = await apiFetch('/v1/global-variables', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const updateGlobalVariable = async (id, payload) => {
  const response = await apiFetch('/v1/global-variables', {
    method: 'PUT',
    body: JSON.stringify({ id, ...payload })
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const deleteGlobalVariable = async (id) => {
  const response = await apiFetch(`/v1/global-variables/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}
