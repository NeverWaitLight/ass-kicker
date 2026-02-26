import { apiFetch } from './api'

export const submitSendTask = async (payload) => {
  const response = await apiFetch('/api/submit', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  return response.json()
}
