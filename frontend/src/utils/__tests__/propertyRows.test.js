import { describe, it, expect } from 'vitest'
import {
  createObjectRow,
  createPropertyRow,
  propertiesToRows,
  rowsToProperties,
  validatePropertyRows,
  RESERVED_KEY
} from '../propertyRows'

describe('propertyRows utils', () => {
  it('maps properties to rows', () => {
    const rows = propertiesToRows({ host: 'smtp', config: { region: 'cn' } })
    expect(rows).toHaveLength(2)
    expect(rows.find((row) => row.key === 'config').type).toBe('object')
  })

  it('maps rows to properties', () => {
    const rows = [
      createPropertyRow({ key: 'host', type: 'string', value: 'smtp' }),
      createPropertyRow({
        key: 'config',
        type: 'object',
        objectRows: [createObjectRow({ key: 'region', value: 'cn' })]
      })
    ]
    const props = rowsToProperties(rows)
    expect(props.host).toBe('smtp')
    expect(props.config.region).toBe('cn')
  })

  it('validates duplicate keys', () => {
    const rows = [
      createPropertyRow({ key: 'host', type: 'string', value: 'a' }),
      createPropertyRow({ key: 'HOST', type: 'string', value: 'b' })
    ]
    const result = validatePropertyRows(rows)
    expect(result.rowInvalidIds.size).toBeGreaterThan(0)
  })

  it('validates reserved keys', () => {
    const rows = [createPropertyRow({ key: RESERVED_KEY, type: 'string', value: 'x' })]
    const result = validatePropertyRows(rows)
    expect(result.rowInvalidIds.size).toBeGreaterThan(0)
  })
})
