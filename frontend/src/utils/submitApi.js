import { unwrapData } from './apiPayload'
import { apiFetch } from './v1'

export const submitSendTask = async (payload) => {
  const response = await apiFetch('/v1/submit', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  const json = await response.json()
  return unwrapData(json)
}
