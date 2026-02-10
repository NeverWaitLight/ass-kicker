## Why

当前暗色模式下的文字颜色在特定背景下（如 #073642）可读性较差，同时界面上的一些按钮标签过于冗长，影响用户体验。本变更旨在改善这些问题，提升整体界面的可读性和易用性。

## What Changes

- 修改暗色模式下特定背景色 #073642 的文字颜色为 #faf4e8，以提高可读性
- 简化界面上的按钮标签，如将"新建通道"、"新建用户"简化为"新建"，"返回列表"简化为"返回"等
- 为通道类型 PUSH 添加中文名称"系统推送"

## Capabilities

### New Capabilities
- `dark-mode-color-adjustment`: 实现暗色模式下特定背景色的文字颜色调整功能
- `button-label-simplification`: 实现按钮标签的简化功能
- `channel-type-i18n-update`: 更新通道类型的国际化配置，添加PUSH类型的中文名称

### Modified Capabilities

## Impact

- 前端界面：暗色模式下的文字颜色和按钮标签
- 国际化配置：通道类型的中文名称
- 用户体验：整体界面的可读性和易用性得到提升