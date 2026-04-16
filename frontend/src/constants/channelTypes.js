export const CHANNEL_TYPE_LABELS = {
  'zh-CN': {
    SMS: 'SMS',
    EMAIL: 'EMAIL',
    IM: 'IM',
    PUSH: 'PUSH'
  },
  'en-US': {
    SMS: 'SMS',
    EMAIL: 'EMAIL',
    IM: 'IM',
    PUSH: 'PUSH'
  }
}

export const CHANNEL_TYPE_VALUES = Object.keys(CHANNEL_TYPE_LABELS['en-US'])

export const buildChannelTypeOptions = (values, t, te) => {
  const list = values && values.length ? values : CHANNEL_TYPE_VALUES
  return list.map((value) => {
    return { value, label: value }
  })
}

export const getChannelTypeLabel = (value, t, te) => {
  if (!value) return ''
  return value
}
