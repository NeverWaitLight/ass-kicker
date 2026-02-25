## Context

当前前端 `ChannelConfigPage.vue` 中定义了通道类型协议列表，其中 `HTTP_API` 协议的显示标签为 "HTTP API"。用户希望将其简化为 "HTTP"，使显示更加简洁。

## Goals / Non-Goals

**Goals:**
- 将 `HTTP_API` 协议在前端下拉选择器中的显示标签从 "HTTP API" 修改为 "HTTP"
- 保持底层协议值 `HTTP_API` 不变，确保与后端 API 兼容

**Non-Goals:**
- 不修改后端 API 或数据结构
- 不修改协议的功能或属性字段
- 不涉及其他通道类型的命名修改

## Decisions

**决策：仅修改显示标签，保持协议值不变**

- **方案**: 修改 `ChannelConfigPage.vue` 中 `protocolConfigs` 数组里 `HTTP_API` 的 `label` 属性
- **理由**: 
  - 最小化变更范围，降低风险
  - 保持与现有后端接口和数据库记录的兼容性
  - 不影响已配置的通道

**备选方案考虑:**
- 修改协议值从 `HTTP_API` 为 `HTTP`：需要后端同步修改，迁移成本高，不采纳

## Risks / Trade-offs

**[风险]** 用户可能对 "HTTP" 的含义产生歧义（与协议本身混淆）
→ **缓解**: 在 UI 上保持清晰的上下文，用户选择的是通道类型而非协议

**[风险]** 现有用户已习惯 "HTTP API" 的显示
→ **缓解**: 名称变更直观，学习成本低
