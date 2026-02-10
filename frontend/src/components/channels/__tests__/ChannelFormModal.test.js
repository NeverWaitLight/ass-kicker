import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import ChannelFormModal from '../ChannelFormModal.vue'
import { message } from 'ant-design-vue'
import i18n from '../../../i18n'

vi.mock('ant-design-vue', () => ({
  message: {
    warning: vi.fn(),
    error: vi.fn()
  }
}))

describe('ChannelFormModal', () => {
  it('emits submit payload with parsed properties', async () => {
    const model = { id: null, name: 'Email', type: 'EMAIL', description: '', properties: {} }
    const wrapper = mount(ChannelFormModal, {
      props: {
        open: true,
        mode: 'create',
        submitting: false,
        model
      },
      global: {
        plugins: [i18n]
      }
    })

    wrapper.vm.propertiesText = '{ "sender": "ops" }'
    await wrapper.vm.handleSubmit()

    const emitted = wrapper.emitted('submit')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0].properties.sender).toBe('ops')
  })

  it('warns when required fields missing', async () => {
    const model = { id: null, name: '', type: '', description: '', properties: {} }
    const wrapper = mount(ChannelFormModal, {
      props: { open: true, mode: 'create', submitting: false, model },
      global: {
        plugins: [i18n]
      }
    })

    await wrapper.vm.handleSubmit()
    expect(message.warning).toHaveBeenCalled()
  })
})
