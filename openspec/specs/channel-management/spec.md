## ADDED Requirements

### Requirement: 通道 CRUD 管理

定义通知渠道（Channel）的管理功能：

- Channel 实体模型包含：id、name、type、description、灵活 KV 属性
- 提供完整的 REST API（/api/channels）进行 CRUD 操作
- 支持灵活 KV 属性存储渠道特定配置
- 支持渠道类型分类（email、sms、push 等）
- 敏感信息必须加密存储
- 前端统一显示为"通道"

#### Scenario: 创建新通道
- **WHEN** 管理员提交通道创建表单
- **THEN** 系统保存通道信息并返回成功响应

#### Scenario: 获取通道列表
- **WHEN** 用户请求通道列表
- **THEN** 系统返回所有可用通道的信息

#### Scenario: 更新通道配置
- **WHEN** 管理员修改通道的 KV 属性
- **THEN** 系统保存更新后的配置

---

### Requirement: 通道类型国际化

定义通道类型的中文显示名称：

- PUSH 渠道类型显示为"系统推送"
- HTTP_API 渠道类型显示为"HTTP"
- 前端下拉框显示中文名称
- API 通信保持原始类型值（英文）

#### Scenario: 前端显示通道类型
- **WHEN** 用户打开通道类型选择下拉框
- **THEN** 显示中文名称而非英文代码

---

### Requirement: 邮件协议配置

支持多种邮件发送协议的配置：

- 邮件协议选择（SMTP、HTTP_API，默认 SMTP）
- SMTP 配置字段自动填充
- 结构化保存邮件协议和 SMTP 配置（protocol 字段 + smtp 对象）

#### Scenario: 选择 SMTP 协议
- **WHEN** 用户选择 SMTP 协议
- **THEN** 表单自动填充 SMTP 相关配置字段

#### Scenario: 保存协议配置
- **WHEN** 用户提交包含协议类型的通道配置
- **THEN** 系统结构化保存 protocol 和 smtp 配置
