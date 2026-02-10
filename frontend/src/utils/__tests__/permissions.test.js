import { describe, it, expect } from 'vitest'
import { hasPermission, hasAnyPermission } from '../permissions'

describe('permissions', () => {
  it('grants all permissions to admin', () => {
    const user = { role: 'ADMIN' }
    expect(hasPermission(user, 'channel:read')).toBe(true)
  })

  it('checks permissions array', () => {
    const user = { role: 'USER', permissions: ['channel:read'] }
    expect(hasPermission(user, 'channel:read')).toBe(true)
    expect(hasPermission(user, 'channel:create')).toBe(false)
  })

  it('supports permission object map', () => {
    const user = { role: 'USER', permissions: { 'channel:update': true } }
    expect(hasPermission(user, 'channel:update')).toBe(true)
  })

  it('checks any permissions', () => {
    const user = { role: 'USER', permissions: ['channel:read'] }
    expect(hasAnyPermission(user, ['channel:create', 'channel:read'])).toBe(true)
  })
})
