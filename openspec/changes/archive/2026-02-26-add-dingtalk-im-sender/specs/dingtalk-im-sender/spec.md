## ADDED Requirements

### Requirement: 钉钉机器人消息发送
系统应当支持通过钉钉机器人 webhook 发送消息，使用 HTTP POST 请求和钉钉开放平台 API 格式。

#### Scenario: 成功发送文本消息
- **WHEN** 用户配置了有效的 webhook URL 并发送消息请求
- **THEN** 系统向钉钉机器人发送 HTTP POST 请求，返回发送成功响应

#### Scenario: 带签名加密的消息发送
- **WHEN** 用户配置了 webhook URL 和密钥
- **THEN** 系统使用 HMAC-SHA256 算法生成签名，并将签名附加到请求中

#### Scenario: 请求超时处理
- **WHEN** 钉钉机器人 API 在超时时间内未响应
- **THEN** 系统返回超时错误，错误码为 TIMEOUT

#### Scenario: 重试机制
- **WHEN** 发送失败且失败原因是可重试的网络错误
- **THEN** 系统按照配置的重试次数和延迟进行重试

#### Scenario: 认证失败处理
- **WHEN** 钉钉机器人返回 401 或 403 错误
- **THEN** 系统不进行重试，直接返回 AUTHENTICATION_FAILED 错误

### Requirement: 钉钉配置类
系统应当提供钉钉机器人发送器的配置类，支持必要的配置属性。

#### Scenario: 创建配置实例
- **WHEN** 用户创建 DingTalkIMSenderConfig 实例
- **THEN** 配置类包含 webhookUrl、secret、timeout、maxRetries 等属性

#### Scenario: 配置参数校验
- **WHEN** 配置的 webhookUrl 为空或空白
- **THEN** 参数校验失败，抛出约束违反异常
