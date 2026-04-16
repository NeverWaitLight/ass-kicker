import { unwrapData } from './apiPayload'
import { apiFetch } from './v1'

export const fetchChannels = async () => {
  const response = await apiFetch('/v1/channels')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const fetchChannel = async (id) => {
  const response = await apiFetch(`/v1/channels/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const fetchChannelTypes = async () => {
  const response = await apiFetch('/v1/channels/types')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const fetchEmailProtocols = async () => {
  const response = await apiFetch('/v1/channels/email-protocols')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const fetchImTypes = async () => {
  const response = await apiFetch('/v1/channels/im-types')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const createChannel = async (payload) => {
  const response = await apiFetch('/v1/channels', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const updateChannel = async (id, payload) => {
  const response = await apiFetch('/v1/channels', {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}

export const deleteChannel = async (id) => {
  const response = await apiFetch(`/v1/channels/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}

export const testSendChannel = async (payload) => {
  const response = await apiFetch('/v1/channels/test-send', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}
