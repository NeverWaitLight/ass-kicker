## ADDED Requirements

### Requirement: 统一 Sender 接口

定义统一的 Sender 抽象接口，支持跨渠道发送消息：

- 提供统一接口支持邮件、短信、推送等多种渠道
- 统一的发送方法签名：输入（recipient、subject、content），输出（status、error 信息）
- 标准化输入对象包含收件人、主题、内容字段
- 标准化输出对象包含发送状态和错误信息
- 接口设计支持扩展新的发送渠道

#### Scenario: 通过 SMTP 发送邮件
- **WHEN** 调用 Sender 接口发送邮件
- **THEN** 系统通过配置的 SMTP 服务器发送邮件

#### Scenario: 发送失败处理
- **WHEN** 发送过程中发生错误
- **THEN** 返回包含错误信息的响应对象

---

### Requirement: SMTP 发送器配置

定义 SMTP 发送器的配置 Schema 和参数映射：

- 配置项包含 key、type、required、default、hint 属性
- 必填参数验证：host、port、username、password
- 默认值设置：protocol（SMTP）、sslEnabled、from 地址、connectionTimeout、readTimeout、maxRetries、retryDelay
- 配置属性正确映射到 SMTP 发送器参数

#### Scenario: 配置参数验证
- **WHEN** 用户提交 SMTP 配置缺少必填参数
- **THEN** 系统返回验证错误

#### Scenario: 默认值应用
- **WHEN** 用户未提供可选参数
- **THEN** 系统使用预设的默认值

---

### Requirement: 阿里云邮件发送器

实现阿里云邮件服务的 Sender 接口：

- 实现 Sender 接口用于阿里云邮件服务
- 支持配置 host、port、username、password 等 SMTP 设置
- 使用 SSL 建立安全连接
- 兼容阿里云邮件推送 API

#### Scenario: 通过阿里云发送邮件
- **WHEN** 配置阿里云邮件发送器并调用发送接口
- **THEN** 系统通过阿里云服务发送邮件

---

### Requirement: 测试发送功能

提供测试发送功能，允许使用临时配置发送测试消息：

- 提供测试发送功能（无需永久保存配置）
- 管理临时配置（测试后清理）
- 记录测试发送日志（userId、configId、channelType、protocol、target、success、messageId、errorReason）
- 支持所有渠道类型的测试发送
- 验证用户权限

#### Scenario: 发送测试邮件
- **WHEN** 用户提供临时 SMTP 配置并点击测试发送
- **THEN** 系统使用临时配置发送邮件并返回结果

#### Scenario: 测试日志记录
- **WHEN** 测试发送完成
- **THEN** 系统记录测试日志供后续查询
