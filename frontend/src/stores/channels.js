import { reactive, ref } from 'vue'

export const channelList = ref([])
export const channelLoading = ref(false)
export const channelError = ref('')

export const channelPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

export const channelSearch = ref('')

export const channelFormState = reactive({
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

export const channelDeleteState = reactive({
  open: false,
  deleting: false,
  target: null
})

export const resetChannelForm = () => {
  channelFormState.current = {
    id: null,
    name: '',
    type: 'SMS',
    description: '',
    properties: {}
  }
}

export const setChannelList = (items) => {
  channelList.value = Array.isArray(items) ? items : []
  channelPagination.total = channelList.value.length
}

export const upsertChannel = (item) => {
  if (!item) return
  const index = channelList.value.findIndex((channel) => channel.id === item.id)
  if (index >= 0) {
    channelList.value.splice(index, 1, item)
  } else {
    channelList.value.unshift(item)
    channelPagination.total = channelList.value.length
  }
}

export const removeChannel = (id) => {
  if (id == null) return
  channelList.value = channelList.value.filter((channel) => channel.id !== id)
  channelPagination.total = channelList.value.length
}
