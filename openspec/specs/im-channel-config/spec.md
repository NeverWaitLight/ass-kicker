## ADDED Requirements

### Requirement: IM 通道类型配置界面
前端应当支持 IM 通道类型的配置界面，允许用户配置钉钉机器人相关属性。

#### Scenario: 显示 IM 通道配置字段
- **WHEN** 用户在通道配置页面选择通道类型为 IM
- **THEN** 页面显示 IM 通道特有的配置字段（webhook URL、密钥等）

#### Scenario: Webhook URL 输入
- **WHEN** 用户在 webhook URL 输入框中输入地址
- **THEN** 系统保存该地址作为通道属性

#### Scenario: 密钥配置（可选）
- **WHEN** 用户配置钉钉机器人的签名密钥
- **THEN** 系统保存密钥用于消息签名加密

#### Scenario: 配置表单验证
- **WHEN** 用户提交 IM 通道配置但 webhook URL 为空
- **THEN** 表单显示验证错误，阻止提交

### Requirement: IM 通道类型标识
前端通道类型常量中应当添加 IM 类型的定义和显示名称。

#### Scenario: 通道类型列表包含 IM
- **WHEN** 用户打开通道类型选择下拉框
- **THEN** 选项列表中包含「即时消息（IM）」选项

#### Scenario: IM 通道类型映射
- **WHEN** 后端返回通道类型为 IM
- **THEN** 前端正确显示为「即时消息」
