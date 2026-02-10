## MODIFIED Requirements

### Requirement: Dark mode text color adjustment for specific background
系统 SHALL 在暗色模式下将主要文字颜色设为 `#faf4e8`，并用于页面区域标题、输入框标题、选择框标题、表头等主要文字。

#### Scenario: 主要文字颜色应用
- **WHEN** 应用处于暗色模式且渲染页面区域标题、输入框标题、选择框标题或表头
- **THEN** 系统 SHALL 使用 `#faf4e8` 渲染主要文字

#### Scenario: 特定背景上的主要文字
- **WHEN** 应用处于暗色模式且背景色为 `#073642`
- **THEN** 系统 SHALL 使用 `#faf4e8` 渲染主要文字

#### Scenario: 对比度合规
- **WHEN** 评估 `#073642` 背景与 `#faf4e8` 文字的可访问性对比度
- **THEN** 系统 SHALL 满足 WCAG AA 对比度要求

## ADDED Requirements

### Requirement: Dark mode normal text color usage for secondary text
系统 SHALL 在暗色模式下将副标题、placeholder、选择框选项、操作按钮等普通文字使用暗色模式普通文字颜色。

#### Scenario: 普通文字颜色应用
- **WHEN** 应用处于暗色模式且渲染副标题、placeholder、选择框选项或操作按钮文字
- **THEN** 系统 SHALL 使用暗色模式普通文字颜色渲染普通文字