import { describe, it, expect, vi } from 'vitest'
import { fetchChannel, fetchChannels, fetchChannelTypes, createChannel, testSendChannel } from '../channelApi'
import { apiFetch } from '../api'

vi.mock('../api', () => ({
  apiFetch: vi.fn()
}))

describe('channelApi', () => {
  it('fetches channel list', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve([{ id: '1', name: 'Email' }])
    })

    const data = await fetchChannels()
    expect(apiFetch).toHaveBeenCalledWith('/api/channels')
    expect(data).toHaveLength(1)
  })

  it('fetches channel detail', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ id: '1', name: 'Email' })
    })

    const data = await fetchChannel('1')
    expect(apiFetch).toHaveBeenCalledWith('/api/channels/1')
    expect(data.name).toBe('Email')
  })

  it('fetches channel types', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(['SMS', 'EMAIL'])
    })

    const data = await fetchChannelTypes()
    expect(apiFetch).toHaveBeenCalledWith('/api/channels/types')
    expect(data).toContain('SMS')
  })

  it('throws error when fetch fails', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: false,
      text: () => Promise.resolve('error')
    })

    await expect(fetchChannels()).rejects.toThrow('error')
  })

  it('creates channel via api', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ id: '2', name: 'SMS' })
    })

    const payload = { name: 'SMS', type: 'SMS', properties: {} }
    const data = await createChannel(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/channels', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    expect(data.id).toBe('2')
  })

  it('tests send channel via api', async () => {
    apiFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ success: true, messageId: 'test-1' })
    })

    const payload = { type: 'EMAIL', target: 'a@b.com', content: 'test', properties: {} }
    const data = await testSendChannel(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/channels/test-send', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    expect(data.success).toBe(true)
  })
})
