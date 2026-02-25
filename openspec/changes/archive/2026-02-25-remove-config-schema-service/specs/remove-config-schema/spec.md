# Remove Config Schema Service Specification

## Purpose

本规范定义了移除配置 Schema 服务的要求。该服务原本用于向前端提供邮件协议配置字段的定义，移除后前端将自行维护配置模板。

## REMOVED Requirements

### Requirement: 获取邮件协议配置 Schema

**Reason**: 配置字段定义属于前端 UI 范畴，后端不应承担此职责。前端自行维护配置模板更加灵活。

**Migration**: 
- 前端需在代码中维护 SMTP 和 HTTP API 的配置模板
- 前端移除对 `/api/channels/email-protocols` 接口的调用
- 后端保留保存和测试通道时的配置校验逻辑

#### Scenario: 获取 SMTP 配置 Schema（已移除）
- **WHEN** 前端请求 `/api/channels/email-protocols` 接口
- **THEN** 该接口已被移除，前端应使用本地维护的配置模板

#### Scenario: 获取 HTTP API 配置 Schema（已移除）
- **WHEN** 前端请求获取 HTTP API 协议的字段定义
- **THEN** 该接口已被移除，前端应使用本地维护的配置模板

### Requirement: SMTP sender 配置 Schema

**Reason**: 同上，配置 Schema 定义已移至前端维护。

**Migration**: 前端参考原 Schema 定义在代码中维护配置模板，包含字段：host、port、username、password、sslEnabled、from、connectionTimeout、readTimeout、maxRetries、retryDelay

#### Scenario: 获取 SMTP 配置字段定义（已移除）
- **WHEN** 前端需要渲染 SMTP 配置表单
- **THEN** 前端使用本地维护的配置模板而非请求后端
