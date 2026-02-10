## Why

为了统一管理不同渠道的消息发送功能，如邮件、短信等，需要创建一个抽象的Sender接口，使系统能够灵活扩展各种消息发送方式。

## What Changes

- 新增Sender抽象接口，定义统一的消息发送方法
- 创建具体实现类，如AliyunMailSender
- 定义统一的入参和返回对象结构
- 集成阿里云邮箱服务作为首个实现

## Capabilities

### New Capabilities
- `sender-interface`: 定义消息发送的抽象接口及其实现规范
- `aliyun-mail-sender`: 实现阿里云邮箱发送功能的具体能力

### Modified Capabilities

## Impact

- 需要在backend模块中新增相关接口和实现类
- 配置文件需要支持邮箱相关参数
- 现有消息发送功能需适配新接口