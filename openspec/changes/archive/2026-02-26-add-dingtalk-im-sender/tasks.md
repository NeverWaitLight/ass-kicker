## 1. 后端实现

- [x] 1.1 实现 `DingTalkIMSenderConfig` 配置类，包含 webhookUrl、secret、timeout、maxRetries 等属性
- [x] 1.2 实现 `DingTalkIMSender` 发送类，参照 `HttpEmailSender` 使用 WebClient 发送消息
- [x] 1.3 实现钉钉签名加密方法（HMAC-SHA256）
- [x] 1.4 创建 `ImSenderConfig` 基类（类比 `EmailSenderConfig`）
- [x] 1.5 更新 `IMSenderType` 枚举（如需要）

## 2. 前端实现

- [x] 2.1 在 `constants/channelTypes.js` 中添加 IM 通道类型定义
- [x] 2.2 在 `ChannelConfigPage.vue` 中添加 `isImChannel` 计算属性
- [x] 2.3 在 `ChannelConfigPage.vue` 中添加 IM 通道配置字段（webhook URL、密钥）
- [x] 2.4 在 `ChannelConfigPage.vue` 中添加 IM 通道的属性 schema 处理逻辑
- [x] 2.5 更新 `sectionHint` 以支持 IM 通道的提示文本

## 3. 测试与验证

- [x] 3.1 编写 `DingTalkIMSender` 单元测试
- [x] 3.2 编写 `DingTalkIMSenderConfig` 验证测试
- [x] 3.3 前端组件测试（ChannelConfigPage IM 通道配置）
- [ ] 3.4 手动测试钉钉机器人消息发送功能
