import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import ObjectValueEditor from '../ObjectValueEditor.vue'

describe('ObjectValueEditor', () => {
  it('emits update when adding row', async () => {
    const wrapper = mount(ObjectValueEditor, {
      props: { rows: [] },
      global: {
        stubs: {
          'a-table': { template: '<div><slot /></div>' },
          'a-input': { template: '<input />' },
          'a-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>'
          }
        }
      }
    })

    await wrapper.get('button').trigger('click')
    const emitted = wrapper.emitted('update:rows')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0]).toHaveLength(1)
  })
})
