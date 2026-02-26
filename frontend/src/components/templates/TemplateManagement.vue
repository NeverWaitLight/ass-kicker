<template>
  <div class="template-management">
    <h2>模板</h2>
    
    <!-- 模板列表 -->
    <div class="template-list-section">
      <h3>模板列表</h3>
      <div class="search-bar">
        <input 
          v-model="searchQuery" 
          placeholder="搜索模板..." 
          @input="searchTemplates"
        />
      </div>
      
      <div class="template-actions">
        <button @click="openCreateModal" class="btn btn-primary" title="新建模板">新建</button>
      </div>
      
      <div class="template-grid">
        <div 
          v-for="template in filteredTemplates" 
          :key="template.id" 
          class="template-card"
        >
          <h4>{{ template.name }}</h4>
          <p>{{ template.description || '无描述' }}</p>
          <div class="template-actions">
            <button @click="viewTemplate(template)" class="btn btn-info">查看</button>
            <button @click="editTemplate(template)" class="btn btn-warning">编辑</button>
            <button @click="deleteTemplate(template.id)" class="btn btn-danger">删除</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 模态框用于创建/编辑模板 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3>{{ isEditing ? '编辑模板' : '创建新模板' }}</h3>
        <form @submit.prevent="saveTemplate">
          <div class="form-group">
            <label for="templateName">模板名称 *</label>
            <input 
              id="templateName"
              v-model="currentTemplate.name" 
              type="text" 
              required
              :disabled="isViewing"
            />
          </div>
          
          <div class="form-group">
            <label for="templateDescription">描述</label>
            <textarea 
              id="templateDescription"
              v-model="currentTemplate.description" 
              :disabled="isViewing"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label for="templateContent">内容 *</label>
            <textarea 
              id="templateContent"
              v-model="currentTemplate.content" 
              rows="10"
              required
              :disabled="isViewing"
            ></textarea>
          </div>
          
          <div class="form-actions" v-if="!isViewing">
            <button type="button" @click="closeModal" class="btn btn-secondary">取消</button>
            <button type="submit" class="btn btn-primary">{{ isEditing ? '更新' : '创建' }}</button>
          </div>
          <div class="form-actions" v-else>
            <button @click="closeModal" class="btn btn-secondary">关闭</button>
            <button @click="startEdit" class="btn btn-warning">编辑</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 查看模板详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content detail-modal" @click.stop>
        <h3>{{ selectedTemplate.name }}</h3>
        <div class="detail-content">
          <p><strong>描述:</strong> {{ selectedTemplate.description || '无描述' }}</p>
          <p><strong>创建时间:</strong> {{ formatTimestamp(selectedTemplate.createdAt) }}</p>
          <p><strong>更新时间:</strong> {{ formatTimestamp(selectedTemplate.updatedAt) }}</p>
          <div class="content-section">
            <strong>内容:</strong>
            <pre>{{ selectedTemplate.content }}</pre>
          </div>
        </div>
        <div class="form-actions">
          <button @click="closeDetailModal" class="btn btn-secondary">关闭</button>
          <button @click="editTemplate(selectedTemplate)" class="btn btn-warning">编辑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { formatTimestamp } from '../../utils/time';

export default {
  name: 'TemplateManagement',
  setup() {
    const templates = ref([]);
    const filteredTemplates = ref([]);
    const searchQuery = ref('');
    const showModal = ref(false);
    const showDetailModal = ref(false);
    const currentTemplate = ref({
      id: null,
      name: '',
      description: '',
      content: ''
    });
    const selectedTemplate = ref({});
    const isEditing = ref(false);
    const isViewing = ref(false);

    // 加载模板列表
    const loadTemplates = async () => {
      try {
        const response = await fetch('/api/templates');
        const data = await response.json();
        templates.value = data;
        filteredTemplates.value = [...data];
      } catch (error) {
        console.error('加载模板失败:', error);
      }
    };

    // 搜索模板
    const searchTemplates = () => {
      if (!searchQuery.value.trim()) {
        filteredTemplates.value = [...templates.value];
      } else {
        filteredTemplates.value = templates.value.filter(template =>
          template.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
          (template.description && template.description.toLowerCase().includes(searchQuery.value.toLowerCase()))
        );
      }
    };

    // 打开创建模态框
    const openCreateModal = () => {
      currentTemplate.value = { id: null, name: '', description: '', content: '' };
      isEditing.value = false;
      isViewing.value = false;
      showModal.value = true;
    };

    // 编辑模板
    const editTemplate = (template) => {
      currentTemplate.value = { ...template };
      isEditing.value = true;
      isViewing.value = false;
      showModal.value = true;
    };

    // 查看模板详情
    const viewTemplate = (template) => {
      selectedTemplate.value = { ...template };
      isViewing.value = true;
      showDetailModal.value = true;
    };

    // 开始编辑详情
    const startEdit = () => {
      editTemplate(selectedTemplate.value);
      closeDetailModal();
    };

    // 保存模板
    const saveTemplate = async () => {
      try {
        const url = currentTemplate.value.id 
          ? `/api/templates/${currentTemplate.value.id}` 
          : '/api/templates';
        const method = currentTemplate.value.id ? 'PUT' : 'POST';
        const payload = {
          name: currentTemplate.value.name,
          description: currentTemplate.value.description,
          content: currentTemplate.value.content
        };
        
        const response = await fetch(url, {
          method: method,
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          closeModal();
          loadTemplates(); // 重新加载列表
        } else {
          console.error('保存模板失败:', await response.text());
        }
      } catch (error) {
        console.error('保存模板时发生错误:', error);
      }
    };

    // 删除模板
    const deleteTemplate = async (id) => {
      if (confirm('确定要删除这个模板吗？')) {
        try {
          const response = await fetch(`/api/templates/${id}`, {
            method: 'DELETE'
          });

          if (response.ok) {
            loadTemplates(); // 重新加载列表
          } else {
            console.error('删除模板失败:', await response.text());
          }
        } catch (error) {
          console.error('删除模板时发生错误:', error);
        }
      }
    };

    // 关闭模态框
    const closeModal = () => {
      showModal.value = false;
    };

    const closeDetailModal = () => {
      showDetailModal.value = false;
    };

    onMounted(() => {
      loadTemplates();
    });

    return {
      templates,
      filteredTemplates,
      searchQuery,
      showModal,
      showDetailModal,
      currentTemplate,
      selectedTemplate,
      isEditing,
      isViewing,
      loadTemplates,
      searchTemplates,
      openCreateModal,
      editTemplate,
      viewTemplate,
      startEdit,
      saveTemplate,
      deleteTemplate,
      closeModal,
      closeDetailModal,
      formatTimestamp
    };
  }
};
</script>

<style scoped>
.template-management {
  padding: 20px;
}

.template-list-section {
  margin-bottom: 30px;
}

.search-bar {
  margin-bottom: 20px;
}

.search-bar input {
  width: 300px;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.template-actions {
  margin-bottom: 20px;
}

.btn {
  padding: 8px 16px;
  margin-right: 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-info {
  background-color: #17a2b8;
  color: white;
}

.btn-warning {
  background-color: #ffc107;
  color: black;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.template-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  background-color: #f9f9f9;
}

.template-card h4 {
  margin-top: 0;
  color: #333;
}

.template-actions {
  margin-top: 15px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 600px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.detail-modal {
  width: 700px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}

.form-actions {
  text-align: right;
  margin-top: 20px;
}

.content-section pre {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}
</style>
