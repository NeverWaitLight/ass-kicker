const RESERVED_KEY = '__value'

let sequence = 0
const nextId = (prefix) => `${prefix}-${Date.now()}-${sequence++}`

export const createPropertyRow = (overrides = {}) => ({
  id: nextId('prop'),
  key: '',
  type: 'string',
  value: '',
  objectRows: [],
  ...overrides
})

export const createObjectRow = (overrides = {}) => ({
  id: nextId('obj'),
  key: '',
  value: '',
  ...overrides
})

const isPlainObject = (value) =>
  value !== null && typeof value === 'object' && !Array.isArray(value)

const normalizeKey = (key) => (key == null ? '' : String(key).trim())

const normalizeValue = (value) => (value == null ? '' : String(value))

export const propertiesToRows = (properties) => {
  if (!isPlainObject(properties)) {
    return [createPropertyRow()]
  }
  const rows = Object.entries(properties).map(([key, value]) => {
    if (isPlainObject(value)) {
      const objectRows = Object.entries(value).map(([childKey, childValue]) =>
        createObjectRow({ key: childKey, value: normalizeValue(childValue) })
      )
      return createPropertyRow({
        key: normalizeKey(key),
        type: 'object',
        objectRows
      })
    }
    return createPropertyRow({
      key: normalizeKey(key),
      type: 'string',
      value: normalizeValue(value)
    })
  })
  return rows.length > 0 ? rows : [createPropertyRow()]
}

export const rowsToProperties = (rows) => {
  const output = {}
  rows.forEach((row) => {
    const key = normalizeKey(row.key)
    if (!key || key === RESERVED_KEY) {
      return
    }
    if (row.type === 'object') {
      const obj = {}
      ;(row.objectRows || []).forEach((child) => {
        const childKey = normalizeKey(child.key)
        if (!childKey || childKey === RESERVED_KEY) {
          return
        }
        obj[childKey] = normalizeValue(child.value)
      })
      output[key] = obj
    } else {
      output[key] = normalizeValue(row.value)
    }
  })
  return output
}

export const validatePropertyRows = (rows) => {
  const rowInvalidIds = new Set()
  const objectInvalidIds = {}
  const objectErrors = {}
  const seen = new Map()

  rows.forEach((row) => {
    const key = normalizeKey(row.key)
    const keyLower = key.toLowerCase()
    if (!key || key === RESERVED_KEY) {
      rowInvalidIds.add(row.id)
    }
    if (key) {
      if (seen.has(keyLower)) {
        rowInvalidIds.add(row.id)
        rowInvalidIds.add(seen.get(keyLower))
      } else {
        seen.set(keyLower, row.id)
      }
    }

    if (row.type === 'object') {
      const invalidSet = new Set()
      const childSeen = new Map()
      ;(row.objectRows || []).forEach((child) => {
        const childKey = normalizeKey(child.key)
        const childLower = childKey.toLowerCase()
        if (!childKey || childKey === RESERVED_KEY) {
          invalidSet.add(child.id)
        }
        if (childKey) {
          if (childSeen.has(childLower)) {
            invalidSet.add(child.id)
            invalidSet.add(childSeen.get(childLower))
          } else {
            childSeen.set(childLower, child.id)
          }
        }
      })
      if (invalidSet.size > 0) {
        objectInvalidIds[row.id] = invalidSet
        objectErrors[row.id] = '对象属性存在空键、保留键或重复键，请检查后再提交。'
      }
    }
  })

  const message =
    rowInvalidIds.size > 0 ? '属性键存在空值、保留键或重复键，请检查后再提交。' : ''

  return { rowInvalidIds, objectInvalidIds, objectErrors, message }
}

export { RESERVED_KEY }
