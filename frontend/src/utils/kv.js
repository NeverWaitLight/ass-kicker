const RESERVED_VALUE_KEY = '__value'

let sequence = 0
const nextId = (prefix) => `${prefix}-${Date.now()}-${sequence++}`

export const createFlatEntry = (overrides = {}) => ({
  id: nextId('flat'),
  key: '',
  value: '',
  ...overrides
})

export const createHierNode = (overrides = {}) => ({
  id: nextId('hier'),
  key: '',
  value: '',
  children: [],
  ...overrides
})

const isPlainObject = (value) =>
  value !== null && typeof value === 'object' && !Array.isArray(value)

const normalizeKey = (key) => (key == null ? '' : String(key).trim())

const normalizeValue = (value) => (value == null ? '' : String(value))

export const splitProperties = (properties) => {
  const flatEntries = []
  const hierNodes = []
  if (!isPlainObject(properties)) {
    return { flatEntries, hierNodes }
  }
  Object.entries(properties).forEach(([key, value]) => {
    if (isPlainObject(value)) {
      hierNodes.push(objectToNode(key, value))
    } else {
      flatEntries.push(createFlatEntry({ key, value: normalizeValue(value) }))
    }
  })
  return { flatEntries, hierNodes }
}

const objectToNode = (key, value) => {
  const normalizedKey = normalizeKey(key)
  const nodeValue = value?.[RESERVED_VALUE_KEY]
  const childrenObject = { ...value }
  delete childrenObject[RESERVED_VALUE_KEY]
  return createHierNode({
    key: normalizedKey,
    value: normalizeValue(nodeValue),
    children: objectToNodes(childrenObject)
  })
}

const objectToNodes = (value) => {
  if (!isPlainObject(value)) {
    return []
  }
  return Object.entries(value).map(([key, childValue]) => {
    if (isPlainObject(childValue)) {
      return objectToNode(key, childValue)
    }
    return createHierNode({ key: normalizeKey(key), value: normalizeValue(childValue), children: [] })
  })
}

const nodesToObject = (nodes) => {
  const output = {}
  nodes.forEach((node) => {
    const key = normalizeKey(node.key)
    if (!key) {
      return
    }
    const childrenObject = nodesToObject(node.children || [])
    const hasChildren = Object.keys(childrenObject).length > 0
    const value = normalizeValue(node.value)
    if (hasChildren) {
      if (value) {
        output[key] = { [RESERVED_VALUE_KEY]: value, ...childrenObject }
      } else {
        output[key] = childrenObject
      }
    } else {
      output[key] = value
    }
  })
  return output
}

const flatEntriesToObject = (entries) => {
  const output = {}
  entries.forEach((entry) => {
    const key = normalizeKey(entry.key)
    if (!key) {
      return
    }
    output[key] = normalizeValue(entry.value)
  })
  return output
}

const findDuplicateKeys = (entries) => {
  const seen = new Map()
  const duplicates = new Set()
  entries.forEach((entry) => {
    const key = normalizeKey(entry.key).toLowerCase()
    if (!key) return
    if (seen.has(key)) {
      duplicates.add(entry.id)
      duplicates.add(seen.get(key))
    } else {
      seen.set(key, entry.id)
    }
  })
  return duplicates
}

const findDuplicateNodes = (nodes, invalidIds = new Set()) => {
  const seen = new Map()
  nodes.forEach((node) => {
    const key = normalizeKey(node.key).toLowerCase()
    if (!key) return
    if (seen.has(key)) {
      invalidIds.add(node.id)
      invalidIds.add(seen.get(key))
    } else {
      seen.set(key, node.id)
    }
  })
  nodes.forEach((node) => {
    if (node.children && node.children.length > 0) {
      findDuplicateNodes(node.children, invalidIds)
    }
  })
  return invalidIds
}

export const validateFlatEntries = (entries) => {
  const invalidIds = new Set()
  entries.forEach((entry) => {
    const key = normalizeKey(entry.key)
    if (!key || key === RESERVED_VALUE_KEY) {
      invalidIds.add(entry.id)
    }
  })
  const duplicates = findDuplicateKeys(entries)
  duplicates.forEach((id) => invalidIds.add(id))
  const message =
    invalidIds.size > 0 ? '基础属性存在空键、保留键或重复键，请检查后再提交。' : ''
  return { invalidIds, message }
}

export const validateHierNodes = (nodes) => {
  const invalidIds = new Set()
const checkNode = (node) => {
  const key = normalizeKey(node.key)
  if (!key || key === RESERVED_VALUE_KEY) {
    invalidIds.add(node.id)
  }
    if (node.children && node.children.length > 0) {
      node.children.forEach(checkNode)
    }
  }
  nodes.forEach(checkNode)
  findDuplicateNodes(nodes, invalidIds)
  const message =
    invalidIds.size > 0 ? '层级属性存在空键、保留键或同级重复键，请检查后再提交。' : ''
  return { invalidIds, message }
}

export const mergeProperties = (flatEntries, hierNodes) => {
  const flatObject = flatEntriesToObject(flatEntries)
  const hierObject = nodesToObject(hierNodes)
  const flatKeys = new Set(Object.keys(flatObject).map((key) => key.toLowerCase()))
  const conflicts = Object.keys(hierObject).filter((key) =>
    flatKeys.has(key.toLowerCase())
  )
  if (conflicts.length > 0) {
    return {
      error: `基础属性与层级属性存在重复键：${conflicts.join('、')}`
    }
  }
  return { properties: { ...flatObject, ...hierObject } }
}

export { RESERVED_VALUE_KEY }
