import { describe, it, expect } from 'vitest'
import {
  createFlatEntry,
  createHierNode,
  mergeProperties,
  splitProperties,
  validateFlatEntries,
  validateHierNodes,
  RESERVED_VALUE_KEY
} from '../kv'

describe('kv utils', () => {
  it('splits properties into flat and hierarchical nodes', () => {
    const input = {
      host: 'smtp',
      nested: {
        [RESERVED_VALUE_KEY]: 'root',
        child: 'value'
      }
    }
    const { flatEntries, hierNodes } = splitProperties(input)
    expect(flatEntries).toHaveLength(1)
    expect(hierNodes).toHaveLength(1)
    expect(hierNodes[0].key).toBe('nested')
  })

  it('merges properties and detects conflicts', () => {
    const flat = [createFlatEntry({ key: 'alpha', value: '1' })]
    const hier = [createHierNode({ key: 'alpha', value: '2' })]
    const result = mergeProperties(flat, hier)
    expect(result.error).toBeTruthy()
  })

  it('validates flat entries', () => {
    const entries = [createFlatEntry({ key: '', value: 'x' })]
    const result = validateFlatEntries(entries)
    expect(result.invalidIds.size).toBeGreaterThan(0)
  })

  it('validates hierarchical nodes', () => {
    const nodes = [createHierNode({ key: '', value: 'x' })]
    const result = validateHierNodes(nodes)
    expect(result.invalidIds.size).toBeGreaterThan(0)
  })
})
