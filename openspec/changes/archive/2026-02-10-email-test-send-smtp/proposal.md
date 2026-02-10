## Why

当前测试发送在邮件通道上无法顺利完成，且缺乏发送过程日志，导致问题难以定位；同时邮件通道缺少协议选择与 SMTP 配置项提示，用户无法按系统要求填写必要参数，测试发送更容易失败。

## What Changes

- 增强测试发送日志：输出 sender 组装、发送执行、服务商返回与异常信息，便于排障。
- 为邮件通道增加协议选择（SMTP/HTTP API），并在选择 SMTP 时自动加载所需参数配置项与默认值提示。
- 测试发送时使用临时配置构建 SMTP sender 实例，发送完成后销毁，避免污染全局配置。

## Capabilities

### New Capabilities
- `smtp-sender-config`: 定义 SMTP sender 所需配置项、默认值与校验规则，并用于前端动态渲染与测试发送构建。

### Modified Capabilities
- `test-send`: 测试发送需输出关键日志并使用临时配置组装 sender，完成后清理。
- `channel-management`: 邮件类型通道配置增加协议选择，并在 SMTP 模式下加载必填配置项。

## Impact

- 后端：测试发送服务、邮件 sender 组装与日志输出、邮件协议配置模型。
- 前端：通道配置页面与测试发送弹窗的协议选择与参数表单渲染。
- API：可能新增/扩展获取邮件协议配置项的接口或返回字段。
