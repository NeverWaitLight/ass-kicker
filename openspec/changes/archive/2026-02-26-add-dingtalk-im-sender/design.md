## Context

当前项目后端采用 Spring WebFlux 响应式编程模型，sender 模块已实现 Email 通道的发送逻辑（`HttpEmailSender` 和 `SmtpEmailSender`）。`sender/im` 目录下已存在空的 `DingTalkIMSender.java` 和 `DingTalkIMSenderConfig.java` 文件。前端已有 `ChannelConfigPage.vue` 支持 EMAIL 通道的配置，需要根据 IM 通道的特性扩展配置界面。

**约束条件：**
- 后端需遵循现有 WebFlux Router/Handler 架构
- 配置类需使用 Jakarta Validation 进行参数校验
- 前端需保持与现有 EMAIL 通道配置一致的用户体验
- 钉钉机器人使用 webhook 方式发送消息，支持签名加密

## Goals / Non-Goals

**Goals:**
- 实现钉钉机器人消息发送功能，支持文本消息
- 实现配置类，支持 webhook URL、密钥等必要配置
- 前端支持 IM 通道类型的配置界面
- 保持与现有代码风格一致

**Non-Goals:**
- 不支持钉钉机器人的 @提及功能（可在后续迭代中添加）
- 不支持消息模板功能（由上层模板模块处理）
- 不修改现有 EMAIL 通道的实现

## Decisions

### 1. 钉钉 Sender 实现方式

**决策：** 参照 `HttpEmailSender` 的实现模式，使用 `WebClient` 发送 HTTP 请求到钉钉机器人 webhook。

**理由：**
- 钉钉机器人 API 基于 HTTP POST 请求
- 项目已有 `HttpEmailSender` 的成熟实现可参考
- WebFlux 的 `WebClient` 提供响应式支持

**替代方案：** 使用钉钉官方 SDK
- 未采用原因：增加额外依赖，且 HTTP API 足够简单

### 2. 配置属性设计

**决策：** 配置类继承自新的 `ImSenderConfig` 基类（类比 `EmailSenderConfig`），定义 `protocol` 字段。

**配置属性：**
- `webhookUrl`: 钉钉机器人 webhook 地址（必填）
- `secret`: 可选的签名密钥（用于 HMAC-SHA256 签名）
- `timeout`: 请求超时时间
- `maxRetries`: 最大重试次数

### 3. 前端配置界面

**决策：** 在 `ChannelConfigPage.vue` 中添加 `isImChannel` 计算属性，当通道类型为 IM 时显示特定的配置字段。

**理由：** 保持与现有 `isEmailChannel` 逻辑一致，最小化代码改动。

## Risks / Trade-offs

**[风险] 钉钉 API 限流** → 缓解：在配置中支持超时和重试策略，实现失败降级

**[风险] 签名加密实现复杂** → 缓解：参照钉钉官方文档实现，添加充分测试

**[风险] 前端配置字段与后端不匹配** → 缓解：确保前后端配置属性名称一致，使用类型安全的 API

## Migration Plan

1. 实现后端 `DingTalkIMSenderConfig` 和 `DingTalkIMSender` 类
2. 实现前端 IM 通道配置界面
3. 测试发送功能
4. 更新文档

**回滚策略：** 删除或注释掉新增的 sender 类和前端配置字段，不影响现有功能。

## Open Questions

- 是否需要支持钉钉机器人的消息类型（markdown、link 等）？当前仅支持 text 类型。
- 是否需要将 IM 通道抽象为通用框架以支持其他 IM 平台（如企业微信、飞书）？
