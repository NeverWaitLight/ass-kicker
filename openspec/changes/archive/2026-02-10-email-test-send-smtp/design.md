## Context

当前测试发送对邮件通道使用的是全局注入的 Sender，无法基于临时配置构建 SMTP 实例，导致用户在测试发送时无法验证自己的 SMTP 配置。测试发送日志仅覆盖开始/结束与异常，缺少 sender 组装、协议选择与服务商返回细节，排障成本高。前端通道配置使用通用 KV 表单，缺少邮件协议选择与 SMTP 必填项提示。

## Goals / Non-Goals

**Goals:**
- 为邮件通道提供协议选择（SMTP/HTTP API），并在选择 SMTP 时自动加载所需配置项与默认值。
- 测试发送基于临时配置动态创建 SMTP sender，发送完成后清理。
- 测试发送过程输出可定位问题的结构化日志，避免敏感信息泄露。

**Non-Goals:**
- 不新增新的邮件协议类型或第三方邮件服务商。
- 不改动通道持久化结构（仍使用 properties 作为配置载体）。
- 不扩展邮件内容功能（模板、附件等）。

## Decisions

1. **SMTP 配置项由后端定义并提供给前端渲染**
   - 选择在后端维护 SMTP 配置 schema（字段 key、类型、是否必填、默认值、提示文本）。
   - 通过新增接口（建议 `GET /api/channels/email-protocols`）返回协议与字段定义，避免破坏现有 `/api/channels/types` 返回值。
   - 理由：确保 SMTP sender 参数来源统一，避免前后端字段不一致。

2. **前端在邮件类型下展示协议选择并自动填充属性行**
   - 选择 SMTP 协议时，自动写入 `protocol` 与 `smtp` 对象字段（host、port、username、password、sslEnabled、from、connectionTimeout、readTimeout、maxRetries、retryDelay 等）。
   - 使用现有 KV/对象编辑器结构，保持 `properties` Map 结构不变。
   - 理由：不改变通道数据模型，复用现有 UI 组件。

3. **测试发送使用临时配置构建 Sender 实例**
   - 基于请求 `properties` 解析出 `EmailSenderProperties`，由工厂创建 `SmtpEmailSender` 或 `HttpApiEmailSender`。
   - 发送完成后销毁实例（若实现 `AutoCloseable` 则显式关闭），并清理临时配置。
   - 理由：确保测试发送严格使用临时配置，不污染全局邮件发送配置。

4. **增强测试发送日志并避免泄露敏感信息**
   - 记录事件：开始、配置解析完成、sender 构建完成、发送结果、异常。
   - 记录字段：userId、configId、channelType、protocol、target、success、messageId、errorReason（不输出密码等敏感字段）。

## Risks / Trade-offs

- [Risk] 前端与后端字段 schema 不一致导致解析失败 → Mitigation：后端提供统一 schema，并在前端使用后端返回值渲染。
- [Risk] 日志输出包含敏感配置 → Mitigation：日志仅输出字段名与必要元信息，不输出明文配置值。
- [Risk] 测试发送频繁构建 sender 带来性能开销 → Mitigation：仅在测试发送时创建，使用 boundedElastic 并限制频率。

## Migration Plan

- 先发布后端接口与动态 sender 组装逻辑，保持兼容。
- 前端上线协议选择与字段自动加载。
- 若需回滚，前端恢复为手动 KV 配置，后端保持旧 sender 行为。

## Open Questions

- 是否需要同步为 HTTP API 协议提供字段模板与前端渲染？若需要，优先与 SMTP 一致提供。
