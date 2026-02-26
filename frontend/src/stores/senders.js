import { reactive, ref } from 'vue'

export const senderList = ref([])
export const senderLoading = ref(false)
export const senderError = ref('')

export const senderPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

export const senderSearch = ref('')

export const senderFormState = reactive({
  open: false,
  mode: 'create',
  submitting: false,
  current: {
    id: null,
    name: '',
    type: 'SMS',
    description: '',
    properties: {}
  }
})

export const senderDeleteState = reactive({
  open: false,
  deleting: false,
  target: null
})

export const resetSenderForm = () => {
  senderFormState.current = {
    id: null,
    name: '',
    type: 'SMS',
    description: '',
    properties: {}
  }
}

export const setSenderList = (items) => {
  senderList.value = Array.isArray(items) ? items : []
  senderPagination.total = senderList.value.length
}

export const upsertSender = (item) => {
  if (!item) return
  const index = senderList.value.findIndex((sender) => sender.id === item.id)
  if (index >= 0) {
    senderList.value.splice(index, 1, item)
  } else {
    senderList.value.unshift(item)
    senderPagination.total = senderList.value.length
  }
}

export const removeSender = (id) => {
  if (id == null) return
  senderList.value = senderList.value.filter((sender) => sender.id !== id)
  senderPagination.total = senderList.value.length
}
