## Why

当前项目有 17 个规范文件夹，存在以下问题：
1. **过度细分**：多个规范是同一能力的实现细节（如 `smtp-sender-config`、`aliyun-mail-sender` 都是 `sender-interface` 的实现）
2. **内容重叠**：`admin-promotion` 实质是 `dynamic-role-assignment` 的特例，两者逻辑重叠
3. **结构不统一**：部分规范包含技术栈 + 业务逻辑（如 `java21-spring-webflux-backend`），部分仅定义单一功能

此变更旨在合并相关规范、减少冗余、统一结构，提升规范的可维护性和清晰度。

## What Changes

- 将 17 个规范文件夹压缩至约 6 个，按**业务领域**而非实现细节组织
- 每个规范聚焦单一业务能力，实现细节作为规范的子内容
- 统一规范结构：技术栈定义与业务逻辑分离
- **移除细碎的 UI 调整规范**（按钮标签、暗色模式颜色），纳入前端规范或 UI 指南

## Capabilities

### 新能力
以下规范将被创建（合并后的领域规范）：

| 新规范名 | 合并来源 | 说明 |
|----------|----------|------|
| `user-system` | `user-auth` + `user-management` + `admin-promotion` + `dynamic-role-assignment` | 统一的用户与权限管理系统 |
| `email-sending` | `sender-interface` + `smtp-sender-config` + `aliyun-mail-sender` + `test-send` | 邮件发送系统（接口 + 实现） |
| `channel-management` | `channel-management` + `channel-type-i18n-update` | 通道管理（含国际化） |
| `observability` | `async-logging` + `request-tracing` | 可观测性（日志 + 追踪） |
| `architecture` | `java21-spring-webflux-backend` + `vue3-ant-design-frontend`（技术栈部分） | 统一的技术栈定义 |
| `ui-components` | `hierarchical-kv-editor` + `button-label-simplification` + `dark-mode-color-adjustment` | UI 组件与样式规范 |

### 修改能力
无（此变更是重组现有规范，不修改需求）

## Impact

- **规范文件**：17 个规范文件夹 → 6 个规范文件夹
- **依赖关系**：规范间的依赖需要重新梳理（如 `email-sending` 依赖 `channel-management`）
- **代码实现**：无需修改代码，仅重组规范文档
- **后续变更**：依赖现有规范名称的变更需要更新引用
