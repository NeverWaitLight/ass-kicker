import { apiFetch } from './api'

export const fetchChannels = async () => {
  const response = await apiFetch('/api/channels')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchChannel = async (id) => {
  const response = await apiFetch(`/api/channels/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchChannelTypes = async () => {
  const response = await apiFetch('/api/channels/types')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchEmailProtocols = async () => {
  const response = await apiFetch('/api/channels/email-protocols')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const createChannel = async (payload) => {
  const response = await apiFetch('/api/channels', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const updateChannel = async (id, payload) => {
  const response = await apiFetch(`/api/channels/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const deleteChannel = async (id) => {
  const response = await apiFetch(`/api/channels/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}

export const testSendChannel = async (payload) => {
  const response = await apiFetch('/api/channels/test-send', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}
