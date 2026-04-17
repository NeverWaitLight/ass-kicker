<template>
  <section class="hier-editor">
    <div v-if="nodes.length === 0" class="hier-editor__empty">
      暂无层级属性，请添加条目。
    </div>
    <div v-else class="hier-editor__list">
      <HierarchicalKvNode
        v-for="node in nodes"
        :key="node.id"
        :node="node"
        :level="0"
        :invalid-ids="invalidIdsSet"
        @update="handleUpdate"
        @add-child="handleAddChild"
        @remove="handleRemove"
      />
    </div>
    <div class="hier-editor__footer">
      <a-button type="dashed" @click="addRoot">添加层级属性</a-button>
      <span v-if="error" class="hier-editor__error">{{ error }}</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import HierarchicalKvNode from './HierarchicalKvNode.vue'
import { createHierNode } from '../../utils/kv'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  invalidIds: { type: [Array, Set], default: () => new Set() },
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:nodes'])

const invalidIdsSet = computed(() =>
  props.invalidIds instanceof Set ? props.invalidIds : new Set(props.invalidIds)
)

const updateNode = (nodes, id, patch) =>
  nodes.map((node) => {
    if (node.id === id) {
      return { ...node, ...patch }
    }
    if (node.children && node.children.length > 0) {
      return { ...node, children: updateNode(node.children, id, patch) }
    }
    return node
  })

const addChild = (nodes, id, child) =>
  nodes.map((node) => {
    if (node.id === id) {
      return { ...node, children: [...(node.children || []), child] }
    }
    if (node.children && node.children.length > 0) {
      return { ...node, children: addChild(node.children, id, child) }
    }
    return node
  })

const removeNode = (nodes, id) =>
  nodes
    .filter((node) => node.id !== id)
    .map((node) =>
      node.children && node.children.length > 0
        ? { ...node, children: removeNode(node.children, id) }
        : node
    )

const handleUpdate = (id, patch) => {
  emit('update:nodes', updateNode(props.nodes, id, patch))
}

const handleAddChild = (id) => {
  emit('update:nodes', addChild(props.nodes, id, createHierNode()))
}

const handleRemove = (id) => {
  emit('update:nodes', removeNode(props.nodes, id))
}

const addRoot = () => {
  emit('update:nodes', [...props.nodes, createHierNode()])
}

 
</script>

<style scoped>
.hier-editor__empty {
  padding: 12px;
  color: #8c8c8c;
  background: #fafafa;
  border-radius: 8px;
}

.hier-editor__footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.hier-editor__error {
  color: #ff4d4f;
  font-size: 12px;
}

</style>
