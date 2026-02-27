import { apiFetch } from './api'

export const listSendRecords = async (page = 1, size = 10) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
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
