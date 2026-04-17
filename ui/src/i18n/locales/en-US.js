import { CHANNEL_TYPE_LABELS } from '../../constants/channelTypes'

export default {
  brand: {
    title: 'Ass Kicker'
  },
  nav: {
    users: 'User management',
    channels: 'Channel management',
    templates: 'Template management',
    sendRecords: 'Send records'
  },
  common: {
    save: 'Save',
    confirm: 'OK',
    cancel: 'Cancel',
    copy: 'Copy',
    copied: 'Copied'
  },
  user: {
    personalSettings: 'Profile',
    changePassword: 'Change password',
    apiKeyManagement: 'API keys',
    newUsername: 'New username',
    oldPassword: 'Current password',
    newPassword: 'New password',
    apiKeyName: 'Label',
    apiKeyNamePlaceholder: 'Optional defaults to Default key',
    createApiKey: 'Create',
    apiKeyColName: 'Label',
    apiKeyColPrefix: 'Key prefix',
    apiKeyColCreated: 'Created',
    apiKeyColAction: 'Actions',
    revokeApiKey: 'Revoke',
    revokeApiKeyConfirm: 'Revoke this API key',
    apiKeyCreated: 'API key created',
    apiKeySaveHint: 'Save it now full key cannot be shown again',
    apiKeySaved: 'Saved',
    defaultApiKeyName: 'Default key',
    usernameRequired: 'Enter a username',
    usernameUpdated: 'Username updated',
    usernameUpdateFailed: 'Could not update username',
    passwordFieldsRequired: 'Enter current and new password',
    passwordUpdated: 'Password updated',
    passwordUpdateFailed: 'Could not update password',
    apiKeyListFailed: 'Could not load API keys',
    apiKeyCreateFailed: 'Could not create API key',
    apiKeyRevoked: 'API key revoked',
    apiKeyRevokeFailed: 'Could not revoke API key',
    logout: 'Sign out',
    fallbackName: 'User'
  },
  theme: {
    dark: 'Dark',
    light: 'Light'
  },
  locale: {
    zhCN: '中文简体',
    enUS: 'English'
  },
  app: {
    deniedChannel: 'Your account does not have permission to open this page'
  },
  channelType: CHANNEL_TYPE_LABELS['en-US']
}
