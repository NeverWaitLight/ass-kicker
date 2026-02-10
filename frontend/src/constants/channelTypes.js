export const CHANNEL_TYPE_LABELS = {
  'zh-CN': {
    SMS: '短信',
    EMAIL: '电子邮件',
    IM: '即时消息',
    PUSH: '系统推送'
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
    const key = `channelType.${value}`
    const label = te && te(key) ? t(key) : value
    return { value, label }
  })
}

export const getChannelTypeLabel = (value, t, te) => {
  if (!value) return ''
  const key = `channelType.${value}`
  return te && te(key) ? t(key) : value
}
