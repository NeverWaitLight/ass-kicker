import { computed, ref } from 'vue'

/**
 * 列表页「新增 / 编辑」弹窗通用状态
 * @returns {{ open: import('vue').Ref<boolean>, currentId: import('vue').Ref<string|null>, modalTitle: import('vue').ComputedRef<string>, openCreate: (reset?: () => void) => void, openEdit: (record: { id?: string }, fill: (record: unknown) => void) => void, close: () => void }}
 */
export function useFormModal() {
  const open = ref(false)
  const currentId = ref(null)

  const modalTitle = computed(() => (currentId.value ? '编辑' : '新增'))

  const openCreate = (reset) => {
    currentId.value = null
    reset?.()
    open.value = true
  }

  const openEdit = (record, fill) => {
    currentId.value = record?.id ?? null
    fill(record)
    open.value = true
  }

  const close = () => {
    open.value = false
    currentId.value = null
  }

  return {
    open,
    currentId,
    modalTitle,
    openCreate,
    openEdit,
    close
  }
}
