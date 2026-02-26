import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import ChannelConfigPage from '../../views/ChannelConfigPage.vue'
import i18n from '../../../i18n'

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('../../../utils/senderApi', () => ({
  createSender: vi.fn(),
  fetchSender: vi.fn(),
  fetchSenderTypes: vi.fn(() => Promise.resolve(['SMS', 'EMAIL', 'IM', 'PUSH'])),
  fetchEmailProtocols: vi.fn(() => Promise.resolve({ defaultProtocol: 'SMTP', protocols: [] })),
  updateSender: vi.fn()
}))

vi.mock('../../../utils/permissions', () => ({
  CHANNEL_PERMISSIONS: {
    create: 'channel:create',
    edit: 'channel:edit'
  },
  hasPermission: vi.fn(() => true)
}))

vi.mock('../../../stores/auth', () => ({
  currentUser: { value: { roles: ['admin'] } }
}))

describe('ChannelConfigPage - IM Channel', () => {
  const createWrapper = () => {
    return mount(ChannelConfigPage, {
      global: {
        plugins: [i18n],
        stubs: {
          ChannelManagementLayout: { template: '<div><slot /></div>' },
          PropertyEditor: { template: '<div class="property-editor" />' },
          ChannelTestSendModal: { template: '<div class="test-modal" />' }
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should show IM type selector when IM channel type is selected', async () => {
    const wrapper = createWrapper()
    await nextTick()

    const typeSelect = wrapper.find('a-select[model-value=""]')
    await typeSelect.vm.$emit('update:value', 'IM')
    await nextTick()

    const imTypeLabel = wrapper.text()
    expect(imTypeLabel).toContain('IM 类型')
  })

  it('should have isImChannel computed property', () => {
    const wrapper = createWrapper()
    
    wrapper.vm.form.type = 'IM'
    
    expect(wrapper.vm.isImChannel).toBe(true)
    
    wrapper.vm.form.type = 'EMAIL'
    expect(wrapper.vm.isImChannel).toBe(false)
  })

  it('should display webhook URL field for DingTalk IM type', async () => {
    const wrapper = createWrapper()
    await nextTick()

    wrapper.vm.form.type = 'IM'
    wrapper.vm.imType = 'DINGTALK'
    await nextTick()

    const sectionHint = wrapper.vm.sectionHint
    expect(sectionHint).toContain('Webhook URL')
  })

  it('should build properties with dingtalk type for IM channel', async () => {
    const wrapper = createWrapper()
    await nextTick()

    wrapper.vm.form.type = 'IM'
    wrapper.vm.imType = 'DINGTALK'
    await nextTick()

    const properties = wrapper.vm.buildProperties()
    
    expect(properties.type).toBe('DINGTALK')
    expect(properties.dingtalk).toBeDefined()
  })

  it('should validate IM channel configuration', async () => {
    const wrapper = createWrapper()
    await nextTick()

    wrapper.vm.form.type = 'IM'
    wrapper.vm.form.name = 'Test IM Channel'
    wrapper.vm.imType = 'DINGTALK'
    await nextTick()

    const isValid = wrapper.vm.validateForm()
    
    expect(isValid).toBe(true)
    expect(wrapper.vm.typeError).toBe('')
    expect(wrapper.vm.nameError).toBe('')
  })

  it('should show error when IM channel type is not selected', async () => {
    const wrapper = createWrapper()
    await nextTick()

    wrapper.vm.form.type = 'IM'
    wrapper.vm.form.name = ''
    await nextTick()

    wrapper.vm.validateForm()
    
    expect(wrapper.vm.nameError).toContain('通道名称不能为空')
  })
})
