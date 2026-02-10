## ADDED Requirements

### Requirement: Email protocol selection for channel configuration
系统 SHALL 在通道类型为 EMAIL 时提供邮件协议选择，并提供默认协议。

#### Scenario: Email protocol selection
- **WHEN** 用户在通道配置中选择类型为 EMAIL
- **THEN** 系统 SHALL 展示协议选择（至少包含 SMTP 与 HTTP_API），并默认选中 SMTP

### Requirement: SMTP configuration fields auto-population
系统 SHALL 在选择 SMTP 协议时自动加载 SMTP 必填配置项与默认值提示。

#### Scenario: Auto-populate SMTP fields
- **WHEN** 用户将邮件协议选择为 SMTP
- **THEN** 系统 SHALL 显示并预填 SMTP 所需字段（host、port、username、password 等）及其默认值提示

### Requirement: SMTP properties structure
系统 SHALL 以结构化方式保存邮件协议与 SMTP 配置。

#### Scenario: Persist SMTP properties
- **WHEN** 用户保存或测试发送邮件通道配置
- **THEN** 系统 SHALL 在 properties 中写入 protocol 字段与 smtp 对象字段，用于后端构建 SMTP sender
