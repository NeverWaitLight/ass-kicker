import { unwrapData, unwrapPage } from './apiPayload'
import { apiFetch } from './v1'

export const listSendRecords = async (page = 1, size = 10, recipient, channelType) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (recipient != null && String(recipient).trim() !== '') {
    params.set('recipient', String(recipient).trim())
  }
  if (channelType != null && String(channelType).trim() !== '') {
    params.set('channelType', String(channelType).trim())
  }
  const response = await apiFetch(`/v1/records?${params.toString()}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapPage(json)
}

export const getSendRecord = async (id) => {
  const response = await apiFetch(`/v1/records/${id}`)
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}
