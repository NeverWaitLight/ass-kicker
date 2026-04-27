import { CHANNEL_TYPE_LABELS } from '../../constants/channelTypes'

export default {
  brand: {
    title: 'Ass Kicker'
  },
  nav: {
    users: 'User management',
    channels: 'Channel management',
    templates: 'Template management',
    globalVariables: 'Global variables',
    sendRecords: 'Send records'
  },
  common: {
    save: 'Save',
    confirm: 'OK',
    cancel: 'Cancel',
    reset: 'Reset',
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
  globalVariable: {
    title: 'Global variables',
    subtitle: 'Manage reusable variables for template rendering and sending',
    searchPlaceholder: 'Search by name or key',
    createTitle: 'New variable',
    editTitle: 'Edit variable',
    ordinal: '#',
    key: 'Key',
    keyPlaceholder: 'e.g. brandName',
    name: 'Name',
    namePlaceholder: 'e.g. Brand name',
    value: 'Value',
    valuePlaceholder: 'Enter a value',
    description: 'Description',
    descriptionPlaceholder: 'Optional',
    status: 'Status',
    enabled: 'Enabled',
    disabled: 'Disabled',
    updatedAt: 'Updated',
    actions: 'Actions',
    edit: 'Edit',
    delete: 'Delete',
    deleteConfirmTitle: 'Delete variable',
    deleteConfirmPrefix: 'Delete variable',
    deleteConfirmSuffix: '? This cannot be undone.',
    keyRequired: 'Enter a variable key',
    nameRequired: 'Enter a variable name',
    valueRequired: 'Enter a variable value',
    created: 'Variable created',
    updated: 'Variable updated',
    deleted: 'Variable deleted',
    loadFailed: 'Could not load global variables',
    saveFailed: 'Could not save',
    deleteFailed: 'Could not delete'
  },
  channelType: CHANNEL_TYPE_LABELS['en-US']
}
