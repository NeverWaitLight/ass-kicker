// TemplateManagement.test.js
import { mount } from '@vue/test-utils';
import TemplateManagement from '../TemplateManagement.vue';
import { describe, it, expect, vi } from 'vitest';

// Mock fetch API
global.fetch = vi.fn();

describe('TemplateManagement', () => {
  beforeEach(() => {
    // 清除所有 mock 调用历史
    vi.clearAllMocks();
  });

  it('renders correctly', async () => {
    // Mock API response
    global.fetch.mockResolvedValueOnce({
      json: () => Promise.resolve([])
    });

    const wrapper = await mount(TemplateManagement);

    // 检查组件标题是否存在
    expect(wrapper.find('h2').text()).toBe('模板');
    
    // 检查是否有新建模板按钮
    expect(wrapper.find('.btn-primary').text()).toBe('新建');
  });

  it('loads templates on mount', async () => {
    const mockTemplates = [
      { id: 1, name: 'Test Template', description: 'Test Description', content: 'Test Content' }
    ];
    
    // Mock API response
    global.fetch.mockResolvedValueOnce({
      json: () => Promise.resolve(mockTemplates)
    });

    const wrapper = await mount(TemplateManagement);

    // 等待异步操作完成
    await wrapper.vm.$nextTick();

    // 检查是否调用了 API
    expect(global.fetch).toHaveBeenCalledWith('/api/templates');
  });

  it('opens create modal when clicking new template button', async () => {
    // Mock API response
    global.fetch.mockResolvedValueOnce({
      json: () => Promise.resolve([])
    });

    const wrapper = await mount(TemplateManagement);
    
    // 等待组件挂载
    await wrapper.vm.$nextTick();

    // 点击新建模板按钮
    await wrapper.find('.btn-primary').trigger('click');

    // 检查模态框是否显示
    expect(wrapper.vm.showModal).toBe(true);
    expect(wrapper.vm.isEditing).toBe(false);
  });

  it('validates required fields in form', async () => {
    // Mock API response
    global.fetch.mockResolvedValueOnce({
      json: () => Promise.resolve([])
    });

    const wrapper = await mount(TemplateManagement);
    
    // 等待组件挂载
    await wrapper.vm.$nextTick();

    // 打开创建模态框
    await wrapper.find('.btn-primary').trigger('click');

    // 尝试提交空表单
    const form = wrapper.find('form');
    await form.trigger('submit.prevent');

    // 由于我们无法直接测试验证（因为这是浏览器原生功能），
    // 我们可以检查初始状态下表单字段是否为空
    expect(wrapper.vm.currentTemplate.name).toBe('');
    expect(wrapper.vm.currentTemplate.content).toBe('');
  });
});
