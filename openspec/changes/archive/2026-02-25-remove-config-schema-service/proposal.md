## Why

当前后端提供 `EmailProtocolSchemaService` 用于通知前端配置字段，但这种设计增加了后端的职责和复杂度。配置字段的定义本质上属于前端 UI 范畴，后端只需在保存或测试通道时按具体配置类进行校验即可。此变更将配置定义的责任归还给前端，简化后端架构。

## What Changes

- **删除** `EmailProtocolSchemaService` 类及其相关 DTO（`EmailProtocolSchemaResponse`）
- **删除** 获取配置 Schema 的 API 端点
- **保留** `EmailSenderPropertyMapper`，仅用于配置属性转换和校验
- **保留** 保存通道和测试通道的 API，校验逻辑内嵌于配置转换过程中
- 前端自行维护通道配置模板，根据通道类型加载对应模板

## Capabilities

### New Capabilities

（无新增能力）

### Modified Capabilities

- `channel-config`: 移除获取配置 Schema 的接口，保留保存和测试通道的校验逻辑

## Impact

- **后端**：
  - 删除 `EmailProtocolSchemaService.java`
  - 删除 `EmailProtocolSchemaResponse.java` DTO
  - 删除相关的 API 端点（如 `GET /api/channels/schema` 或类似）
- **前端**：
  - 需要在代码中维护通道配置模板（SMTP、HTTP API 等）
  - 移除调用 Schema API 获取配置字段的逻辑
- **API**：
  - 移除获取配置 Schema 的接口（**BREAKING**）
