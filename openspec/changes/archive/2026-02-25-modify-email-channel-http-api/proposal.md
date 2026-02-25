## Why

当前前端在通道类型选择中将 "HTTP_API" 协议显示为 "HTTP API"，为了保持命名简洁性和一致性，需要将其修改为 "HTTP"。

## What Changes

- 前端通道类型选择器中，将 "HTTP API" 标签修改为 "HTTP"
- 保持底层协议值 `HTTP_API` 不变，仅修改显示名称

## Capabilities

### New Capabilities

### Modified Capabilities
- `channel-type-i18n-update`: 通道类型显示名称的修改，属于国际化/显示层面的变更

## Impact

- 前端代码：`frontend/src/views/ChannelConfigPage.vue` 中的通道类型标签
- 不影响后端 API 和数据结构
- 不影响现有的通道配置功能
