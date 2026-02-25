## Context

当前后端实现了 `EmailProtocolSchemaService` 用于向前端提供邮件协议配置字段的 Schema 定义，包括 SMTP 和 HTTP API 两种协议的字段名、标签、类型、是否必填、默认值和占位符等信息。前端通过调用 `/api/channels/email-protocols` 接口获取这些 Schema 来动态生成配置表单。

这种设计的问题：
- 配置字段定义本质上是 UI 层面的关注点，后端不应承担此职责
- 增加了后端的复杂度和维护成本
- Schema 变更需要后端发布，不够灵活

## Goals / Non-Goals

**Goals:**
- 删除 `EmailProtocolSchemaService` 及其相关 DTO
- 删除 `/api/channels/email-protocols` API 端点
- 删除 `ChannelHandler` 和 `ChannelRouter` 中对 Schema 服务的依赖
- 保留 `EmailSenderPropertyMapper` 的配置转换和校验功能
- 确保保存通道和测试通道时的校验逻辑正常工作

**Non-Goals:**
- 不修改 `EmailSenderPropertyMapper` 的校验逻辑
- 不修改通道配置的数据结构（`SmtpEmailSenderConfig`、`HttpEmailSenderConfig`）
- 不涉及前端代码的修改（前端需自行维护配置模板）
- 不修改其他通道类型（SMS、IM、PUSH）的配置处理逻辑

## Decisions

### 决策 1：删除 Schema 服务及相关代码

**选择**：完全删除 `EmailProtocolSchemaService` 和 `EmailProtocolSchemaResponse`

**理由**：
- 这些类仅用于向前端提供配置字段定义，删除后不影响核心业务逻辑
- 配置模板由前端维护更加灵活，前端可以根据 UI 需求自定义字段展示

**替代方案考虑**：
- 方案 A：保留但标记为废弃 → 拒绝，因为会增加技术债务
- 方案 B：移至前端代码 → 这是前端的职责，但不由本次变更执行

### 决策 2：删除 API 端点 `/api/channels/email-protocols`

**选择**：从 `ChannelRouter` 和 `ChannelHandler` 中移除该端点

**理由**：
- 该端点唯一用途是返回 Schema，删除服务后端点无存在意义
- **BREAKING CHANGE**：前端需要移除对该接口的调用

### 决策 3：保留 `EmailSenderPropertyMapper` 的校验逻辑

**选择**：不做任何修改，保留现有校验行为

**理由**：
- 校验逻辑在保存通道和测试通道时执行，是后端的核心职责
- 配置数据的有效性验证不能依赖前端，后端必须校验

## Risks / Trade-offs

**[风险] 前端需要适配**
- 删除 API 端点是破坏性变更，前端需要移除相关调用并自行维护配置模板
- **缓解**：在提交前与前端开发者沟通，或在 API 文档中标注为废弃

**[风险] 前后端配置属性不一致**
- 前端模板字段与后端配置类属性名必须保持一致，否则配置无法正确转换
- **缓解**：通过文档或类型定义明确约定配置属性名

**[Trade-off] 失去后端统一管理配置定义的能力**
- 优点：前端可以灵活定制配置表单
- 缺点：配置字段变更时，前后端需要手动同步

## Migration Plan

### 后端迁移步骤

1. 删除 `EmailProtocolSchemaService.java`
2. 删除 `EmailProtocolSchemaResponse.java`
3. 修改 `ChannelHandler.java`：
   - 移除 `EmailProtocolSchemaService` 依赖注入
   - 删除 `listEmailProtocols` 方法
4. 修改 `ChannelRouter.java`：
   - 移除 `/api/channels/email-protocols` 路由定义
   - 移除 `EmailProtocolSchemaResponse` 导入

### 回滚策略

如需回滚，恢复以上删除/修改的文件即可，无数据迁移风险。

## Open Questions

无
