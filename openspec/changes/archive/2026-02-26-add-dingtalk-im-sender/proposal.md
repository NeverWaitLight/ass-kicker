## Why

当前项目中 `sender/im` 目录下已存在空的 `DingTalkIMSender.java` 和 `DingTalkIMSenderConfig.java` 文件，需要参照 `email` 文件夹下 `HttpEmailSender` 的实现风格，完成钉钉机器人通道的 sender 配置实现，并同步在前端添加对应的配置功能，以支持用户通过钉钉机器人发送通知消息。

## What Changes

- 在后端实现 `DingTalkIMSender` 类，参照 `HttpEmailSender` 的 WebFlux 风格实现钉钉机器人消息发送逻辑
- 实现 `DingTalkIMSenderConfig` 配置类，定义钉钉机器人所需的配置属性（如 webhook URL、密钥等）
- 在前端 `ChannelConfigPage.vue` 中添加 IM 通道类型（钉钉机器人）的配置界面
- 更新前端通道类型常量，添加 IM 类型的显示名称

## Capabilities

### New Capabilities
- `dingtalk-im-sender`: 钉钉机器人消息发送能力，支持通过钉钉机器人 webhook 发送消息
- `dingtalk-im-frontend-config`: 前端钉钉机器人通道配置界面，支持 webhook URL 等属性配置

### Modified Capabilities

## Impact

- 后端：`sender/im` 目录下的 Java 类实现
- 前端：`ChannelConfigPage.vue` 视图组件、通道类型常量
- 依赖：可能需要添加钉钉机器人 SDK 或 HTTP 客户端依赖
- API：通道配置 API 将支持新的 IM 类型及其属性
