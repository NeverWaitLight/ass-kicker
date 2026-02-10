export const CHANNEL_PERMISSIONS = {
  view: 'channel:read',
  create: 'channel:create',
  edit: 'channel:update',
  remove: 'channel:delete'
}

export const hasPermission = (user, permission) => {
  if (!user || !permission) return false
  if (user.role === 'ADMIN') return true
  const rawPermissions = user.permissions || user.scopes || user.authorities
  if (Array.isArray(rawPermissions)) {
    return rawPermissions.includes(permission)
  }
  if (rawPermissions && typeof rawPermissions === 'object') {
    return !!rawPermissions[permission]
  }
  return false
}

export const hasAnyPermission = (user, permissions) => {
  if (!permissions || permissions.length === 0) return true
  return permissions.some((permission) => hasPermission(user, permission))
}
