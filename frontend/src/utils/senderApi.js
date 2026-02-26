import { apiFetch } from './api'

export const fetchSenders = async () => {
  const response = await apiFetch('/api/senders')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchSender = async (id) => {
  const response = await apiFetch(`/api/senders/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchSenderTypes = async () => {
  const response = await apiFetch('/api/senders/types')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const fetchEmailProtocols = async () => {
  const response = await apiFetch('/api/senders/email-protocols')
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const createSender = async (payload) => {
  const response = await apiFetch('/api/senders', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const updateSender = async (id, payload) => {
  const response = await apiFetch(`/api/senders/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const deleteSender = async (id) => {
  const response = await apiFetch(`/api/senders/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return true
}

export const testSendSender = async (payload) => {
  const response = await apiFetch('/api/senders/test-send', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}
