/**
 * 时间格式化工具函数
 * 后端统一存储13位UTC时间戳，前端展示时转换为本地化时间
 */

/**
 * 将13位UTC时间戳格式化为本地化时间字符串
 * @param {number} timestamp - 13位UTC时间戳
 * @returns {string} 格式化后的本地时间字符串，格式为 YYYY/MM/DD HH:mm:ss
 */
export const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-'

  const date = new Date(timestamp)

  // 验证时间戳是否有效
  if (isNaN(date.getTime())) {
    return '-'
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

/**
 * 将13位UTC时间戳格式化为日期字符串（不含时间）
 * @param {number} timestamp - 13位UTC时间戳
 * @returns {string} 格式化后的日期字符串，格式为 YYYY/MM/DD
 */
export const formatDate = (timestamp) => {
  if (!timestamp) return '-'

  const date = new Date(timestamp)

  if (isNaN(date.getTime())) {
    return '-'
  }

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
