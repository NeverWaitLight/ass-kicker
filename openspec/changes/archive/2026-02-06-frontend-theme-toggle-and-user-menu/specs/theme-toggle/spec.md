## ADDED Requirements

### Requirement: Theme Toggle Component
系统应提供一个主题切换组件，允许用户在light和dark模式之间切换。

#### Scenario: 用户切换主题
- **WHEN** 用户点击主题切换开关
- **THEN** 系统应立即切换到所选主题，并将选择保存到本地存储

### Requirement: Theme Persistence
系统应记住用户选择的主题偏好，并在用户下次访问时应用该主题。

#### Scenario: 用户返回网站
- **WHEN** 用户再次访问网站
- **THEN** 系统应根据本地存储的主题偏好应用相应的主题

### Requirement: Theme Application
系统应正确地将选定的主题应用到所有页面元素。

#### Scenario: 主题切换完成
- **WHEN** 用户切换主题后
- **THEN** 所有页面元素应相应地更新其外观以匹配所选主题