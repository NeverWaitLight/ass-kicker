## ADDED Requirements

### Requirement: SMTP sender configuration schema
系统 SHALL 定义 SMTP sender 的配置 schema，包含字段 key、类型、是否必填、默认值与提示信息，并可供前端渲染使用。

#### Scenario: 获取 SMTP 配置 schema
- **WHEN** 前端请求邮件协议配置 schema
- **THEN** 系统 SHALL 返回 SMTP 协议的字段定义（至少包含 host、port、username、password 及其必填/默认值信息）

### Requirement: SMTP sender 参数校验与默认值
系统 SHALL 在构建 SMTP sender 时校验必填参数，并为可选参数填充默认值。

#### Scenario: 校验与默认值填充
- **WHEN** 系统基于测试发送的临时配置构建 SMTP sender
- **THEN** 系统 SHALL 校验 host、port、username、password 必填，且为 protocol、sslEnabled、from、connectionTimeout、readTimeout、maxRetries、retryDelay 设定默认值（若未提供）

### Requirement: SMTP sender 参数映射
系统 SHALL 将 properties 中的 SMTP 配置映射为 SMTP sender 构建所需的结构化参数。

#### Scenario: 参数映射
- **WHEN** properties 中包含 protocol 与 smtp 对象配置
- **THEN** 系统 SHALL 将其映射为 SMTP sender 所需参数并用于实例化
