import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import HierarchicalKvEditor from '../HierarchicalKvEditor.vue'

describe('HierarchicalKvEditor', () => {
  it('emits update when adding root node', async () => {
    const wrapper = mount(HierarchicalKvEditor, {
      props: { nodes: [] },
      global: {
        stubs: {
          'a-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>'
          },
          'a-space': { template: '<span><slot /></span>' }
        }
      }
    })

    await wrapper.get('button').trigger('click')
    const emitted = wrapper.emitted('update:nodes')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0]).toHaveLength(1)
  })
})
