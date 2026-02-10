## Why

当前系统中的channel类型没有固定枚举值限制，导致数据不一致问题。通过限定channel类型为SMS、EMAIL、IM、PUSH四种固定类型，可以确保数据的一致性和系统的稳定性。

## What Changes

- 修改Java枚举类，将channel类型限定为SMS、EMAIL、IM、PUSH四种固定值
- 后端从端口到数据库的逻辑需要支持这个枚举
- 数据库中仍然使用字符串类型存储
- 前端界面将文字输入框替换为单选框，仅允许选择预定义的四种类型
- 对新增和修改channel的操作添加校验，确保只能选择这四种类型之一

## Capabilities

### Modified Capabilities
- `channel-management`: 修改channel类型的定义和校验规则，限定为SMS、EMAIL、IM、PUSH四种固定类型

## Impact

- Java后端代码中的ChannelType枚举类需要修改
- 数据库表结构保持不变（仍使用字符串类型）
- REST API接口需要更新以支持新的枚举类型
- 前端表单组件需要从文本输入改为单选框
- 现有channel数据需要确保符合新的枚举值