import { apiFetch } from './v1'

export const createApiKey = async (payload) => {
  const response = await apiFetch('/v1/api-keys', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const listApiKeys = async () => {
  const response = await apiFetch('/v1/api-keys')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const revokeApiKey = async (id) => {
  const response = await apiFetch(`/v1/api-keys/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}
