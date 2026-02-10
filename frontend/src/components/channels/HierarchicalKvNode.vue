<template>
  <div class="hier-node" :style="{ marginLeft: level * 16 + 'px' }">
    <div class="hier-node__row">
      <a-input
        class="hier-node__key"
        :value="node.key"
        placeholder="键"
        :status="invalidIds.has(node.id) ? 'error' : ''"
        @update:value="onUpdateKey"
      />
      <a-input
        class="hier-node__value"
        :value="node.value"
        placeholder="值"
        @update:value="onUpdateValue"
      />
      <a-space size="small">
        <a-button size="small" @click="onAddChild">添加子级</a-button>
        <a-button size="small" danger @click="onRemove">删除</a-button>
      </a-space>
    </div>
    <div v-if="node.children && node.children.length" class="hier-node__children">
      <HierarchicalKvNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :invalid-ids="invalidIds"
        @update="emitUpdate"
        @add-child="emitAddChild"
        @remove="emitRemove"
      />
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'HierarchicalKvNode' })

const props = defineProps({
  node: { type: Object, required: true },
  level: { type: Number, required: true },
  invalidIds: { type: Object, required: true }
})

const emit = defineEmits(['update', 'add-child', 'remove'])

const onUpdateKey = (value) => emit('update', props.node.id, { key: value })
const onUpdateValue = (value) => emit('update', props.node.id, { value })
const onAddChild = () => emit('add-child', props.node.id)
const onRemove = () => emit('remove', props.node.id)

const emitUpdate = (...args) => emit('update', ...args)
const emitAddChild = (...args) => emit('add-child', ...args)
const emitRemove = (...args) => emit('remove', ...args)
</script>

<style scoped>
.hier-node {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  background: #ffffff;
}

.hier-node__row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hier-node__key,
.hier-node__value {
  min-width: 160px;
  flex: 1;
}

.hier-node__children {
  margin-top: 12px;
}
</style>
