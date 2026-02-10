## Why

当前前端显示的channel都显示为通道，配置通道时用户体验不佳，需要先选择类型，然后填充名字和配置属性。属性以KV单元格形式展示，避免用户直接填写JSON，单元格需要支持层级填充功能。

## What Changes

- 修改前端UI显示逻辑，将channel统一显示为"通道"
- 实现新的通道配置界面，支持类型选择、名称填写
- 设计KV单元格形式的属性配置界面
- 支持层级填充功能，提升配置体验
- 后端提供相应的API接口支持

## Capabilities

### New Capabilities
- `channel-config-ui`: 实现通道配置的前端界面，包括类型选择、名称填写和KV属性配置
- `hierarchical-kv-editor`: 实现支持层级填充的KV编辑器组件

### Modified Capabilities

## Impact

- 前端UI组件需要重构以支持新的通道配置界面
- 后端API可能需要调整以支持新的配置方式
- 现有的通道配置流程将被替换为新实现