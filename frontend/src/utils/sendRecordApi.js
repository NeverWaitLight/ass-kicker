import { apiFetch } from './api'

export const listSendRecords = async (page = 1, size = 10, recipient, channelType) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (recipient != null && String(recipient).trim() !== '') {
    params.set('recipient', String(recipient).trim())
  }
  if (channelType != null && String(channelType).trim() !== '') {
    params.set('channelType', String(channelType).trim())
  }
  const response = await apiFetch(`/api/send-records?${params.toString()}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}

export const getSendRecord = async (id) => {
  const response = await apiFetch(`/api/send-records/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}
