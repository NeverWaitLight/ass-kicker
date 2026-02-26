import { describe, it, expect, vi } from 'vitest'
import { fetchSender, fetchSenders, fetchSenderTypes, createSender, testSendSender } from '../senderApi'
import { apiFetch } from '../api'

vi.mock('../api', () => ({
  apiFetch: vi.fn()
}))

describe('senderApi', () => {
  it('fetches sender list', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve([{ id: '1', name: 'Email' }])
    })

    const data = await fetchSenders()
    expect(apiFetch).toHaveBeenCalledWith('/api/senders')
    expect(data).toHaveLength(1)
  })

  it('fetches sender detail', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ id: '1', name: 'Email' })
    })

    const data = await fetchSender('1')
    expect(apiFetch).toHaveBeenCalledWith('/api/senders/1')
    expect(data.name).toBe('Email')
  })

  it('fetches sender types', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(['SMS', 'EMAIL'])
    })

    const data = await fetchSenderTypes()
    expect(apiFetch).toHaveBeenCalledWith('/api/senders/types')
    expect(data).toContain('SMS')
  })

  it('throws error when fetch fails', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: false,
      text: () => Promise.resolve('error')
    })

    await expect(fetchSenders()).rejects.toThrow('error')
  })

  it('creates sender via api', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ id: '2', name: 'SMS' })
    })

    const payload = { name: 'SMS', type: 'SMS', properties: {} }
    const data = await createSender(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/senders', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    expect(data.id).toBe('2')
  })

  it('tests send sender via api', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ success: true, messageId: 'test-1' })
    })

    const payload = { type: 'EMAIL', target: 'a@b.com', content: 'test', properties: {} }
    const data = await testSendSender(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/senders/test-send', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    expect(data.success).toBe(true)
  })
})
