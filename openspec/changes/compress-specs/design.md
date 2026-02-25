## Context

当前项目有 17 个规范文件夹，存在以下问题：

**现状**：
- 规范数量过多（17 个），结构分散
- 部分规范是同一能力的实现细节（如邮件发送相关 4 个规范）
- 内容重叠（如 `admin-promotion` 与 `dynamic-role-assignment`）
- 技术栈与业务逻辑混杂（如 `java21-spring-webflux-backend`）

**约束**：
- 保持规范的核心需求不变，仅重组结构
- 确保合并后的规范保持原有功能的完整性
- 使用简体中文编写所有规范文档

**利益相关者**：
- 开发团队：需要清晰的规范指导实现
- 代码审查者：需要规范的依赖关系明确

## Goals / Non-Goals

**Goals:**
- 将 17 个规范压缩至 6 个领域规范
- 每个规范聚焦单一业务能力
- 统一规范结构（技术栈与业务逻辑分离）
- 建立清晰的规范依赖关系

**Non-Goals:**
- 不修改现有功能的需求定义
- 不重构代码实现
- 不引入新的技术栈或框架
- 不修改前端 UI 的具体实现细节

## Decisions

### 1. 规范合并策略

**决策**：按业务领域合并，而非按技术层次

| 合并后规范 | 来源规范 | 理由 |
|------------|----------|------|
| `user-system` | `user-auth` + `user-management` + `admin-promotion` + `dynamic-role-assignment` | 统一的用户与权限领域 |
| `email-sending` | `sender-interface` + `smtp-sender-config` + `aliyun-mail-sender` + `test-send` | 接口与实现属于同一领域 |
| `channel-management` | `channel-management` + `channel-type-i18n-update` | 国际化是通道管理的自然部分 |
| `observability` | `async-logging` + `request-tracing` | 同属可观测性领域 |
| `architecture` | `java21-spring-webflux-backend` + `vue3-ant-design-frontend` | 统一的技术栈定义 |
| `ui-components` | `hierarchical-kv-editor` + `button-label-simplification` + `dark-mode-color-adjustment` | UI 组件与样式统一 |

**替代方案考虑**：
- 方案 A：保持现有规范，仅添加索引文件 → rejected，无法解决结构分散问题
- 方案 B：完全扁平化为单一规范 → rejected，失去模块边界的清晰度

### 2. 规范结构统一

**决策**：采用三段式结构

```
specs/<domain>/spec.md
├── Requirements（需求定义）
├── Design（设计细节，可选）
└── Implementation Notes（实现注释，可选）
```

**理由**：
- 需求与设计分离，便于独立演进
- 实现细节作为可选部分，避免规范过于冗长

### 3. 依赖关系管理

**决策**：显式声明规范间依赖

```
architecture → 基础技术栈
    ↓
user-system, channel-management, observability → 核心业务
    ↓
email-sending, ui-components → 应用层
```

**理由**：
- 清晰的依赖关系有助于理解系统架构
- 便于后续变更影响分析

## Risks / Trade-offs

**[规范合并后历史追溯困难]** → 在合并后的规范中保留原始规范引用，记录变更历史

**[依赖关系变化导致其他变更失效]** → 在提交前检查所有依赖现有规范的变更，更新引用

**[规范过大导致维护困难]** → 每个规范控制在合理篇幅（<50 行需求），必要时拆分子章节

**[团队适应新结构需要时间]** → 更新 README 文档，提供规范映射表
