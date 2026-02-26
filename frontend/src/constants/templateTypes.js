export const CONTENT_TYPE_OPTIONS = [
  { value: 'PLAIN_TEXT', label: '纯文本' },
  { value: 'HTML', label: 'HTML' },
  { value: 'JSON', label: 'JSON' }
]

export const getContentTypeLabel = (value) => {
  const opt = CONTENT_TYPE_OPTIONS.find((o) => o.value === value)
  return opt ? opt.label : value || '纯文本'
}
