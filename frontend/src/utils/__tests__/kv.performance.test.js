import { describe, it, expect } from 'vitest'
import { createHierNode, validateHierNodes } from '../kv'

const buildWideTree = (count) => {
  const nodes = []
  for (let i = 0; i < count; i += 1) {
    nodes.push(createHierNode({ key: `key-${i}`, value: `value-${i}` }))
  }
  return nodes
}

describe('kv performance', () => {
  it('validates large hierarchical data within a reasonable time', () => {
    const nodes = buildWideTree(1500)
    const start = performance.now()
    const result = validateHierNodes(nodes)
    const duration = performance.now() - start
    expect(result.invalidIds.size).toBe(0)
    expect(duration).toBeLessThan(500)
  })
})
