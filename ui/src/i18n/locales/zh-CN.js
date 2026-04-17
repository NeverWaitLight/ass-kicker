import { CHANNEL_TYPE_LABELS } from '../../constants/channelTypes'

export default {
  brand: {
    title: 'Ass Kicker'
  },
  nav: {
    users: '用户管理',
    channels: '通道管理',
    templates: '模板管理',
    sendRecords: '发送记录'
  },
  common: {
    save: '保存',
    confirm: '确认',
    cancel: '取消',
    copy: '复制',
    copied: '已复制'
  },
  user: {
    personalSettings: '个人设置',
    changePassword: '修改密码',
    apiKeyManagement: 'API Key 管理',
    newUsername: '新用户名',
    oldPassword: '旧密码',
    newPassword: '新密码',
    apiKeyName: '备注名',
    apiKeyNamePlaceholder: '可选默认为默认密钥',
    createApiKey: '创建',
    apiKeyColName: '备注名',
    apiKeyColPrefix: 'Key 前缀',
    apiKeyColCreated: '创建时间',
    apiKeyColAction: '操作',
    revokeApiKey: '销毁',
    revokeApiKeyConfirm: '确认销毁此 API Key',
    apiKeyCreated: 'API Key 已创建',
    apiKeySaveHint: '请妥善保存此后再也无法查看完整 Key',
    apiKeySaved: '我已保存',
    defaultApiKeyName: '默认密钥',
    usernameRequired: '请输入用户名',
    usernameUpdated: '用户名已更新',
    usernameUpdateFailed: '更新用户名失败',
    passwordFieldsRequired: '请输入完整密码信息',
    passwordUpdated: '密码已更新',
    passwordUpdateFailed: '更新密码失败',
    apiKeyListFailed: '获取 API Key 列表失败',
    apiKeyCreateFailed: '创建 API Key 失败',
    apiKeyRevoked: 'API Key 已销毁',
    apiKeyRevokeFailed: '销毁 API Key 失败',
    logout: '退出登录',
    fallbackName: '用户'
  },
  theme: {
    dark: '暗',
    light: '亮'
  },
  locale: {
    zhCN: '中文简体',
    enUS: 'English'
  },
  app: {
    deniedChannel: '当前账号暂无权限访问该页面'
  },
  channelType: CHANNEL_TYPE_LABELS['zh-CN']
}
