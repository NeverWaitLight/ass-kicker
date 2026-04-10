/**
 * 单笔 JSON：{ code, message, data }
 */
export function unwrapData(json) {
  if (json != null && typeof json === 'object' && json.code === '200' && 'data' in json) {
    return json.data
  }
  return json
}

/**
 * 分页 JSON：列表在 data，total 等在顶层
 */
export function unwrapPage(json) {
  if (json != null && typeof json === 'object' && json.code === '200' && Array.isArray(json.data)) {
    return {
      items: json.data,
      total: Number(json.total) || 0,
      page: json.page,
      size: json.size
    }
  }
  return {
    items: json.items || json.data || [],
    total: json.total ?? 0,
    page: json.page,
    size: json.size
  }
}
