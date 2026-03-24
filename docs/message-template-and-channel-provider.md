[TOC]

## Templates

### SMS

#### Channel Config

##### ALIYUN

```json
{
  "name": "阿里云短信",
  "key": "aliyun-sms-prod",
  "type": "SMS",
  "provider": "ALIYUN",
  "description": "Aliyun SMS",
  "priorityAddressRegex": "^1[3-9]\\d{9}$",
  "excludeAddressRegex": "^1(70|71|162)\\d{8}$",
  "properties": {
    "accessKeyId": "...",
    "accessKeySecret": "...",
    "signName": "签名",
    "regionId": "cn-hangzhou",
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

##### TENCENT

```json
{
  "name": "腾讯云短信",
  "key": "tencent-sms-prod",
  "type": "SMS",
  "provider": "TENCENT",
  "description": "TENCENT SMS",
  "priorityAddressRegex": "^\\+861[3-9]\\d{9}$|^1[3-9]\\d{9}$",
  "excludeAddressRegex": "^\\+86170\\d{8}$|^170\\d{8}$",
  "properties": {
    "accessKeyId": "...",
    "accessKeySecret": "...",
    "signName": "签名",
    "regionId": "cn-hangzhou",
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

#### Template Config

```json
{
  "code": "captcha",
  "channelType": "SMS",
  "channels": {
    "aliyun": {
      "templateCode": "SMS_123456789"
    },
    "tencent": {
      "templateId": "1234567"
    }
  },
  "templates": {
    "zh-cn": {
      "content": "{$channel.signName}这是登录验证码 {$code}"
    },
    "en": {
      "content": "{$channel.signName}This is your captcha {$code}"
    }
  }
}
```

短信模板分两层：`templates.<locale>.content` 为逻辑文案，`channels.<provider>` 为供应商模板标识映射。运行时先渲染变量，再按选中的 provider 取对应模板 ID / code；若后端采用自建短信网关直发文本，可省略 `channels`。

### Email

#### Channel Config

##### SMTP

```json
{
  "name": "SMTP 邮件",
  "key": "smtp-email-prod",
  "type": "EMAIL",
  "provider": "SMTP",
  "description": "SMTP channel",
  "priorityAddressRegex": "^[a-zA-Z0-9._%+-]+@(mail\\.)?corp\\.example\\.com$",
  "excludeAddressRegex": "^(no-?reply|bounce|mailer-daemon)@",
  "properties": {
    "host": "smtp.example.com",
    "port": 465,
    "username": "user@example.com",
    "password": "pass",
    "sslEnabled": true,
    "from": "noreply@example.com",
    "connectionTimeout": 5000,
    "readTimeout": 10000,
    "maxRetries": 3,
    "retryDelay": 1000
  }
}
```

`SmtpEmailChannel` 使用 `MimeMessageHelper.setText(..., false)`，当前按纯文本发送；若后续要支持 HTML，建议显式增加 `contentType` 或按 provider 单独扩展 `channels`

##### HTTP

```json
{
  "name": "HTTP 邮件",
  "key": "http-email-prod",
  "type": "EMAIL",
  "provider": "HTTP",
  "description": "HTTP API email",
  "priorityAddressRegex": "^[a-zA-Z0-9._%+-]+@team\\.example\\.org$",
  "excludeAddressRegex": "@(tempmail\\.example|disposable\\.test)$",
  "properties": {
    "baseUrl": "https://api.example.com",
    "apiKeyHeader": "Authorization",
    "apiKey": "Bearer secret",
    "from": "noreply@example.com",
    "maxRetries": 3,
    "timeout": 5000,
    "retryDelay": 1000
  }
}
```

请求体字段与 `HttpEmailChannel.buildRequestBody` 一致：`to`、`subject`、`content`，可选 `from`；发送请求里传入的 `attributes` 会并入 JSON，请勿把动态属性硬编码进模板

#### Template Config

```json
{
  "code": "captcha",
  "channelType": "EMAIL",
  "templates": {
    "zh-cn": {
      "subject": "验证码",
      "content": "这是你的验证码 {$code}"
    },
    "en": {
      "subject": "Verification code",
      "content": "This is your verification code {$code}"
    }
  }
}
```

### IM

#### Channel Config

##### Slack

```json
{
  "name": "Slack Bot 渠道",
  "key": "slack-bot-prod",
  "type": "IM",
  "provider": "SLACK",
  "priorityAddressRegex": "^(C|G|D)[A-Z0-9]{8,}$|^#[a-z0-9._-]+$",
  "excludeAddressRegex": "^D0[A-Z0-9]+$|^#archive-",
  "properties": {
    "botToken": "xoxb-...",
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

鉴权使用 Bot Token，形态常为 `xoxb-...`，放在 HTTP `Authorization` 头：`Authorization: Bearer <slack_bot_token>`。发送消息一般需要 `chat:write` scope。

**Request**

```bash
curl -X POST 'https://slack.com/api/chat.postMessage' \
  -H 'Authorization: Bearer xoxb-your-bot-token' \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "C1234567890",
    "text": "Hello from Slack bot"
  }'
```

- `<slack_bot_token>`：与 `properties.botToken` 对应，本服务若接入 Slack 应在请求头使用 `Bearer`
- `channel`：目标会话标识，当前约定由发送请求的 recipient 在运行时注入，支持 channel / group / DM ID，或符合规则的频道名
- `text`：纯文本正文；若使用 `blocks`，该字段常作通知与无障碍 fallback，建议仍提供
- 请求体可带 `blocks` 结构化块、`thread_ts` 线程回复、`reply_broadcast` 是否广播到主频道等；`text` 建议控制在约 4000 字以内以免截断
- Slack 也支持 `application/x-www-form-urlencoded`，实操优先 `application/json`

##### Telegram

```json
{
  "name": "Telegram Bot 渠道",
  "key": "telegram-bot-prod",
  "type": "IM",
  "provider": "TELEGRAM",
  "priorityAddressRegex": "^-?\\d{6,}$|^@[A-Za-z][A-Za-z0-9_]{4,}$",
  "excludeAddressRegex": "^@spam_demo_channel$|^blocked_user_\\d+$",
  "properties": {
    "botToken": "123456:ABCDEF",
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

鉴权：Token 放在 URL 路径 `https://api.telegram.org/bot<token>/METHOD_NAME`，不使用 `Authorization` 头。发送文本对应 `sendMessage`。

**Request**

```bash
curl -X POST 'https://api.telegram.org/bot123456:ABCDEF/sendMessage' \
  -H 'Content-Type: application/json' \
  -d '{
    "chat_id": 123456789,
    "text": "Hello from Telegram bot"
  }'
```

- `bot<token>`：与 `properties.botToken` 拼接进 URL；泄漏即凭证泄漏
- `chat_id`：目标 chat ID 或频道用户名如 `@channelusername`，当前约定由发送请求的 recipient 在运行时注入
- `text`：解析实体后约 1–4096 字符；可用 `parse_mode`（如 `HTML`、`MarkdownV2`）、`reply_markup` 内联键盘、`disable_notification` 静默发送等

#### Template Config

```json
{
  "code": "ops_alert",
  "channelType": "IM",
  "channels": {
    "slack": {
      "text": "{$template.title}: {$template.body}"
    },
    "telegram": {
      "text": "<b>{$template.title}</b>\n{$template.body}",
      "parse_mode": "HTML"
    }
  },
  "templates": {
    "zh-cn": {
      "title": "告警",
      "body": "服务 {$service} 出现异常"
    },
    "en": {
      "title": "Alert",
      "body": "Service {$service} has an issue"
    }
  }
}
```

IM 模板中的 `channels` 只描述 provider payload 片段，不固化收件人字段；`channel` / `chat_id` 这类目标地址由发送请求的 recipient 在运行时注入。

### Push

#### Channel Config

##### APNs

```json
{
  "name": "苹果推送",
  "key": "apns-app-dev",
  "type": "PUSH",
  "provider": "APNS",
  "priorityAddressRegex": "^[0-9a-fA-F]{64}$",
  "excludeAddressRegex": "^0{64}$|^[fF]{64}$",
  "properties": {
    "teamId": "APPLE_TEAM_ID",
    "keyId": "AUTH_KEY_ID",
    "bundleId": "com.example.app",
    "p8KeyContent": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
    "production": false,
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

**Request**

```bash
curl -sS --http2 \
  --header "authorization: bearer <jwt>" \
  --header "apns-topic: <bundleId>" \
  --header "apns-push-type: alert" \
  --header "apns-priority: 10" \
  --header "Content-Type: application/json" \
  --data '{"aps":{"alert":{"title":"可选标题","body":"正文"},"sound":"default"}}' \
  "https://api.push.apple.com/3/device/<device_token>"
```

- `<jwt>`：用上方通道配置里的 `p8KeyContent`（或 `p8KeyPath`）、`keyId`、`teamId` 生成 APNs 要求的 ES256 JWT，本服务在 `APNsPushChannel.buildJwt` 里签发，约 1 小时有效
- `<bundleId>`：即 `properties.bundleId`，对应请求头 `apns-topic`
- `<device_token>`：调用本系统发送接口时的收件人（recipient），即客户端注册远程通知后拿到的设备 token
- `--data` 中 `aps.alert.title` / `body`：分别对应发送请求的主题与正文（无主题时可不写 `title`），`sound` 与代码中一致为 `default`
- `apns-push-type`、`apns-priority`、`Content-Type`：与本服务实现固定一致，一般无需改
- `properties.production=false` 时应切到 APNs sandbox endpoint；`true` 时使用正式 endpoint，上面的 URL 仅演示正式环境

##### FCM

```json
{
  "name": "谷歌推送",
  "key": "fcm-app-prod",
  "type": "PUSH",
  "provider": "FCM",
  "priorityAddressRegex": "^[A-Za-z0-9_-]{140,}$",
  "excludeAddressRegex": "^(INVALID|dummy):token$|^test:fcm:",
  "properties": {
    "serviceAccountJson": {
      "type": "service_account",
      "project_id": "your-project-id",
      "private_key_id": "your-private-key-id",
      "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
      "client_email": "firebase-adminsdk-xxxxx@your-project-id.iam.gserviceaccount.com",
      "client_id": "123456789012345678901"
    },
    "projectId": "your-project-id",
    "maxRetries": 3,
    "timeout": 10000,
    "retryDelay": 1000
  }
}
```

**Request**

```bash
curl -sS -X POST \
  --header "Authorization: Bearer <access_token>" \
  --header "Content-Type: application/json" \
  --data '{"message":{"token":"<fcm_token>","notification":{"title":"可选标题","body":"正文"}}}' \
  "https://fcm.googleapis.com/v1/projects/<project_id>/messages:send"
```

- `<access_token>`：用上方通道配置里的 `serviceAccountJson` 对象通过 Google OAuth 换取短期访问令牌，scope 为 `https://www.googleapis.com/auth/firebase.messaging`，本服务在 `FCMPushChannel.getAccessToken` 中完成（参见 [Authorize send requests](https://firebase.google.com/docs/cloud-messaging/auth-server)）
- `<project_id>`：优先取 `properties.projectId`，否则取 `serviceAccountJson.project_id`，与 `FCMPushChannel.resolveProjectId` 一致
- `<fcm_token>`：调用本系统发送接口时的收件人（recipient），即 FCM registration token
- `--data` 中 `notification.title` / `body`：分别对应发送请求的主题与正文（无主题时实现里不写入 `title` 字段，手写 curl 时可从 JSON 中省略 `title`）
- `Authorization`、`Content-Type`：与本服务 `WebClient` 调用一致；成功时 HTTP `200`，响应 JSON 含 `name`（形如 `projects/<project_id>/messages/0:...`），见 [FCM HTTP v1 API](https://firebase.google.com/docs/cloud-messaging/send-message)

#### Template Config

```json
{
  "code": "new_message_push",
  "channelType": "PUSH",
  "channels": {
    "apns": {
      "aps": {
        "alert": {
          "title": "{$title}",
          "body": "{$body}"
        },
        "sound": "default",
        "badge": 1
      },
      "chatId": "{$chatId}",
      "messageId": "{$msgId}"
    },
    "fcm": {
      "message": {
        "notification": {
          "title": "{$title}",
          "body": "{$body}"
        },
        "data": {
          "chatId": "{$chatId}",
          "messageId": "{$msgId}"
        }
      }
    }
  },
  "templates": {
    "zh-cn": {
      "title": "新消息",
      "body": "对方昵称:消息摘要..."
    },
    "en": {
      "title": "New Message",
      "body": "nickname:shour message..."
    }
  }
}
```

Push 模板中的 `channels` 只描述 payload 片段，不固化 `device token`；APNs 的 device token 与 FCM 的 `message.token` 均由发送请求的 recipient 在运行时注入。

---

## 数据库和对象设计

以下将上文 **Templates** 节中的 JSON 示例映射为 MongoDB 文档模型，并参考 MongoDB 官方文档中**复合唯一索引**等实践建议索引策略。

### 设计原则

- **通道与模板分集合存放**：通道含密钥与连接参数，模板为业务文案与占位符，生命周期与权限不同，便于独立审计与缓存
- **多态 `properties` 与 `channels` 子文档**：不同 `provider` 字段结构不同，用文档模型直接承载上文中的 JSON，避免关系型多表拆行
- **业务键 + 复合唯一**：模板侧用 `code` 与 `channelType`（及可选 `tenantId`）保证唯一；通道侧用稳定业务 `key`（及可选 `tenantId`）保证唯一，`name` 仅作展示
- **敏感字段**：`properties` 内密钥建议在应用层加密或采用 KMS，库内仅存密文或引用；查询索引勿包含明文密钥大字段
- **收件人地址规则**：通道顶层可选 `priorityAddressRegex` 与 `excludeAddressRegex`（与上文 Channel Config 一致），用于按收件人标识（手机号、邮箱、频道 ID、设备 token 等）做优先匹配或排除；空串或未设置表示不参与该规则
- **模板只存内容，不固化收件人**：`channels` 子文档仅保存 provider payload 片段；`recipient`、`channel`、`chat_id`、`device token` 等目标地址始终由发送请求在运行时注入
- **统一命名与大小写**：`channelType` 固定使用大写枚举值；模板内引用通道属性时直接使用真实字段名，如 `{$channel.signName}`

### notification_channels

#### 文档语义

对应上文中各类 **Channel Config**：顶层为通道元数据，含可选的收件人 **优先地址正则** 与 **排除地址正则**（字段名 `priorityAddressRegex` `excludeAddressRegex`），`properties` 为供应商相关键值，结构与上文示例一致即可随 `provider` 变化。

#### 推荐字段

| 字段                      | 类型     | 说明                                                                              |
| ------------------------- | -------- | --------------------------------------------------------------------------------- |
| `_id`                     | ObjectId | 默认主键                                                                          |
| `tenantId`                | string   | 可选，多租户隔离键                                                                |
| `key`                     | string   | 必填，稳定业务标识（如 `aliyun-sms-prod`），用于配置引用与唯一约束                |
| `name`                    | string   | 展示名，与上文 `name` 一致                                                        |
| `type`                    | string   | 枚举：`SMS` `EMAIL` `IM` `PUSH`                                                   |
| `provider`                | string   | 枚举：`ALIYUN` `SMTP` `HTTP` `SLACK` `TELEGRAM` `APNS` `FCM` 等                   |
| `description`             | string   | 可选                                                                              |
| `routeOrder`              | int      | 可选，数值越小优先级越高；用于多个候选通道命中时的确定性排序                      |
| `priorityAddressRegex`    | string   | 可选，优先地址正则；收件人匹配则优先选用本通道（与业务选路策略配合）              |
| `excludeAddressRegex`     | string   | 可选，排除地址正则；收件人匹配则本通道不参与发送                                  |
| `enabled`                 | bool     | 是否启用，上文未写，运维常用                                                      |
| `properties`              | object   | 供应商配置，见上文各 Channel Config 示例；Java 侧可映射为 `JsonNode` 承载任意嵌套 |
| `createdAt` / `updatedAt` | date     | 审计时间                                                                          |

#### Java 类示意（仅类与字段）

```java
class NotificationChannel {
    String id;
    String tenantId;
    String key;
    String name;
    String type;
    String provider;
    String description;
    Integer routeOrder;
    String priorityAddressRegex;
    String excludeAddressRegex;
    boolean enabled;
    JsonNode properties;
    Instant createdAt;
    Instant updatedAt;
}
```

`properties` 的 `JsonNode` 与同文档下文「集合二」`NotificationTemplate` 小节中对 Jackson `JsonNode` 的释义一致，用于与 MongoDB 嵌套文档对齐

#### `priorityAddressRegex` / `excludeAddressRegex`（与上文 Channel Config 对齐）

- 与 `properties` 同级，存于通道文档顶层；语义为 Java `Pattern` 可编译的正则字符串，具体匹配目标为发送请求中的收件人字段（短信为号码、邮件为地址、IM 为 channel/chat 标识、Push 为 device token 等）
- 正则建议在保存配置时预编译校验；校验失败应拒绝写入，而不是等到发送时再报错
- 通道选路规则固定如下：
  1. 先按 `type` + `enabled=true` 初筛候选通道
  2. recipient 命中 `excludeAddressRegex` 的通道直接排除
  3. 若存在命中 `priorityAddressRegex` 的通道，仅在该子集内继续排序；若不存在，则在未排除通道中继续
  4. 按 `routeOrder` 升序排序；未设置视为 `0`
  5. `routeOrder` 相同时按 `key` 升序兜底，保证同样输入得到确定性结果

#### `properties` 形态（与上文 Channel Config 对齐）

- **SMS + ALIYUN**：`accessKeyId` `accessKeySecret` `signName` `regionId` `maxRetries` `timeout` `retryDelay`
- **EMAIL + SMTP**：`host` `port` `username` `password` `sslEnabled` `from` `connectionTimeout` `readTimeout` `maxRetries` `retryDelay`
- **EMAIL + HTTP**：`baseUrl` `apiKeyHeader` `apiKey` `from` `maxRetries` `timeout` `retryDelay`
- **IM + SLACK**：`botToken` `maxRetries` `timeout` `retryDelay`
- **IM + TELEGRAM**：`botToken` `maxRetries` `timeout` `retryDelay`
- **PUSH + APNS**：`teamId` `keyId` `bundleId` `p8KeyContent`（或扩展 `p8KeyPath`）`production` `maxRetries` `timeout` `retryDelay`
- **PUSH + FCM**：`serviceAccountJson`（对象）`projectId` `maxRetries` `timeout` `retryDelay`

#### 索引建议

- 复合唯一：单租户场景建议 `{ key: 1 }` unique；多租户场景建议 `{ tenantId: 1, key: 1 }` unique
- 列表筛选：`{ type: 1, provider: 1, enabled: 1 }`
- `priorityAddressRegex` `excludeAddressRegex` 为选路元数据，通常不作为索引键，除非存在稳定按收件人模式列举通道的查询
- 若常用 `_id` 外键引用，可不再为 `_id` 单独建索引

#### MongoDB 校验（可选）

可用 `validator` 要求 `key` `type` `provider` `name` `properties` 存在；`priorityAddressRegex` `excludeAddressRegex` 为可选；`properties` 内部因多态较宽，适合在应用层校验。

### notification_templates

#### 文档语义

对应上文中 **Template Config**：`code` + `channelType` 标识一类通知模板；`templates` 为按语言的文案与子字段；部分通道在上文中还有按供应商的载荷模板 `channels`（IM、Push）。

#### 推荐字段

| 字段                      | 类型     | 说明                                                                                            |
| ------------------------- | -------- | ----------------------------------------------------------------------------------------------- |
| `_id`                     | ObjectId | 默认主键                                                                                        |
| `tenantId`                | string   | 可选，多租户隔离键                                                                              |
| `code`                    | string   | 业务编码，如 `captcha` `ops_alert` `new_message_push`                                           |
| `channelType`             | string   | 固定大写：`SMS` `EMAIL` `IM` `PUSH`                                                             |
| `templates`               | object   | 键为 locale（如 `zh-cn` `en`），值为该语言下的模板字段；Java 侧可映射为 `JsonNode` 承载任意嵌套 |
| `channels`                | object   | 可选；IM / Push 等供应商片段；Java 侧可映射为 `JsonNode` 与上文结构一致                         |
| `metadata`                | object   | 可选；版本号、说明、负责人                                                                      |
| `createdAt` / `updatedAt` | date     | 审计时间                                                                                        |

#### Java 类示意（仅类与字段）

```java
class NotificationTemplateMetadata {
    String version;
    String description;
    String owner;
}

class NotificationTemplate {
    String id;
    String tenantId;
    String code;
    String channelType;
    JsonNode templates;
    JsonNode channels;
    NotificationTemplateMetadata metadata;
    Instant createdAt;
    Instant updatedAt;
}
```

`JsonNode` 指 Jackson 的树模型（如 `com.fasterxml.jackson.databind.JsonNode`），与 MongoDB 中的对象/嵌套文档一一对应；`templates` 与 `channels` 的具体键名与下级形状见本节随后「与上文 Template Config 对齐」各小节。

#### `templates` 各 `channelType` 下子文档形状（与上文 Template Config 对齐）

- **SMS**：每个 locale 至少 `content`（字符串，含 `{$var}` 占位符）；若对接模板型短信 provider，再配合顶层 `channels.<provider>.templateCode/templateId`
- **EMAIL**：每个 locale `subject` + `content`；当前建议默认为纯文本，若要引入 HTML，需补充 `contentType` 或 provider 级扩展
- **IM**：每个 locale 常见 `title` + `body`（供 `channels.slack` / `channels.telegram` 中占位符引用）
- **PUSH**：每个 locale `title` + `body`，与 `channels.apns` / `channels.fcm` 中占位符对应

#### `channels` 形态（与上文 Template Config 对齐）

- `channels` 的 key 统一使用 provider 的小写别名，如 `aliyun` `tencent` `slack` `telegram` `apns` `fcm`
- **SMS**：可选 `aliyun`（如 `templateCode`）、`tencent`（如 `templateId`），用于绑定供应商模板标识
- **IM**：`slack`（如 `text`）、`telegram`（如 `text` `parse_mode`）；目标地址字段 `channel` / `chat_id` 不落库，由发送请求注入
- **PUSH**：`apns`（含 `aps` 与子占位符、`chatId` `messageId` 等扩展字段）、`fcm`（含 `message.notification` `message.data`）；目标地址字段 `device token` / `message.token` 不落库，由发送请求注入

EMAIL 若后续需要 provider 特有载荷，也可按相同模式扩展 `channels.smtp` / `channels.http`。

#### 索引建议

- 复合唯一：`{ code: 1, channelType: 1 }` unique（多租户时 `{ tenantId: 1, code: 1, channelType: 1 }` unique）
- 若只按 `code` 全局唯一，可改为 `{ code: 1 }` unique，但需业务确认不同 `channelType` 是否允许同 `code`

#### MongoDB 校验（可选）

要求 `code` `channelType` `templates` 存在；`templates` 至少包含一个 locale；具体 locale 内必填字段按 `channelType` 分规则可在应用层实现更清晰。

### 集合间关系

- 发送时：先按固定选路规则选择 **通道文档**（`type`/`enabled`/地址规则/`routeOrder`），再加载 **模板文档**（按 `tenantId` + `code` + `channelType` 或单租户下 `code` + `channelType`），将 locale 层文案与 `channels` 中供应商片段合并渲染
- 不建议把完整通道密钥嵌入模板文档；模板中 `{$channel.signName}` 等占位符在运行时从已选通道的 `properties` 注入即可
- `recipient` 始终来自发送请求，不存入模板；模板持久化内容应保持与调用层 DTO 解耦

---

## 从开发落地视角补充审查意见

以下意见基于当前仓库已有实现链路审查，重点关注“这份设计是否能直接指导开发落地”。

### 1. 模板渲染结果模型还不闭环

当前设计把 `templates`、`channels`、provider payload 片段都描述出来了，但没有定义**最终渲染结果对象**。现有实现里 `TemplateManager.fill(...)` 只返回单个 `renderedContent`，发送链路也只消费一段正文；而本文档已经要求支持：

- Email 的 `subject` + `content`
- IM 的 `title` / `body` 拼装或 provider 定制 `text`
- Push 的 `aps` / `message.notification` / `message.data`

如果没有补一个明确的运行时结果模型，例如 `RenderedMessage { subject, content, providerPayload, attributes }`，开发阶段就无法判断模板引擎输出给发送器的边界，容易在 `TemplateManager`、`SendTaskExecutor`、各 `Channel` 实现之间反复返工。

### 2. 发送请求契约不足以承载新设计

文档默认“先渲染变量，再按 provider 取对应 payload 片段”，但没有同步定义发送入口需要补哪些上下文。现有发送链路只有 `templateCode`、`language`、`params`、`recipients`，并且最终组装 `MsgReq` 时只传了 `recipient` 和单段 `content`，`subject` 固定为空，`attributes` 也未进入主发送链路。

这会直接导致下面几类能力无法落地：

- Email 模板中的 `subject`
- Push 模板中的 `title`
- HTTP Email 里需要动态并入请求体的扩展字段
- IM / Push provider payload 中需要引用调用时上下文的字段

建议在文档里明确发送链路契约：哪些字段来自模板，哪些字段来自请求，哪些字段来自通道，以及它们在运行时如何组装。

### 3. SMS 的“模板归属”存在冲突

本文档把短信供应商模板标识放在 `notification_templates.channels.<provider>` 下，但当前实现里的短信通道本身要求配置 `templateCode` 或 `templateId`，发送器也是按“通道配置 + 全文内容单变量”工作。

这里存在一个必须提前定下来的实现问题：短信 provider 模板 ID 到底归模板管理，还是归通道管理。

- 如果归模板管理，发送时必须先选定 provider，再读取模板里的 provider 绑定
- 如果归通道管理，当前文档里的 `channels.aliyun.templateCode` / `channels.tencent.templateId` 就会和通道配置重复

这个点如果不先统一，开发阶段会出现模板侧和通道侧双写、配置来源冲突、历史数据难迁移的问题。

### 4. 通道模型与当前代码差异较大，缺少迁移说明

文档里的目标模型新增了 `key`、`provider`、`enabled`、`routeOrder`、`priorityAddressRegex`、`excludeAddressRegex` 等字段，并把选路规则改成了确定性排序；但当前实现的 `ChannelEntity` / 前端表单 / API 只支持：

- `name`
- `type`
- `description`
- `includeRecipientRegex`
- `excludeRecipientRegex`
- `properties`

同时当前 `ChannelManager.selectChannel(...)` 是“按 type 过滤后，再按规则过滤，最后随机选一个”。这和文档里的“priority 子集 + routeOrder + key 兜底排序”不是同一套行为。

建议文档补齐迁移策略：

- 老字段如何迁移到新字段
- `includeRecipientRegex` 与 `priorityAddressRegex` 的兼容关系
- 现有随机选路何时下线
- 前端配置页和后端 CRUD DTO 需要如何调整

### 5. provider 矩阵与现有实现不一致

当前仓库已实现的 IM provider 是 `DINGTALK` / `WECOM`，而文档写的是 `SLACK` / `TELEGRAM`；这不是简单补示例，而是 provider 矩阵发生了变化。若这是目标态设计，文档需要明确以下事项：

- 现有 `DINGTALK` / `WECOM` 是继续保留，还是被替换
- `IMChannelSpecConverter`、`ChannelFactory`、前端枚举和测试发送页是否都要同步改
- 发送记录、配置导入、测试数据是否需要兼容旧 provider

否则开发会误判为“只需补文档中的 provider”，但实际是一次跨后端、前端、存量配置的完整改造。

### 6. 若干字段名与当前实现不对齐

文档中有些配置字段与现有 `Spec`/`Converter` 不一致，直接按文档开发或录入配置会失败，建议统一：

- 腾讯云短信当前实现使用 `secretId` / `secretKey` / `sdkAppId` / `region`，不是文档中的 `accessKeyId` / `accessKeySecret` / `regionId`
- HTTP Email 当前实现要求 `path`，文档示例未体现这个必填字段
- FCM 当前实现的 `serviceAccountJson` 是字符串形式的 JSON 内容或路径语义，不是文档中的对象结构
- locale 当前代码使用 `zh-CN`、`en` 等 `Language` 枚举值，文档示例写的是 `zh-cn`

这些命名差异如果不收敛，配置转换层和管理后台都会出现不必要的兼容代码。

### 7. locale 归一化与回退策略缺失

文档允许 `templates.<locale>` 自由扩展，但没有说明 locale 的标准化规则，也没有规定缺少精确语言时如何回退。实现时至少要明确：

- `zh-cn`、`zh-CN`、`ZH_CN` 是否视为同一个 locale
- 请求传 `zh-CN` 但模板只配置了 `zh-cn` 时是否命中
- 若目标 locale 缺失，是否允许回退到默认语言，例如 `zh-CN -> zh -> en`

这个规则不先写清楚，模板查询、索引设计、前端校验和缓存 key 都会各自实现一套。

### 8. 模板存储与现有后台能力之间有明显断层

当前系统模板管理是两层模型：模板主表 + 语言内容表，语言内容接口也是按纯文本维护。本文档改成了单文档里的 `templates` + `channels` 嵌套结构后，实际还需要补以下落地内容：

- 数据迁移脚本
- 模板 CRUD API 重构
- 前端模板编辑器改造，支持结构化 JSON 而不是单段文本
- 历史发送记录中 `templateCode`、渲染结果与新结构的兼容策略

建议把这部分作为单独的小节写出来，否则实现工作量会被明显低估。

### 9. 运行时缓存失效策略没有写

通道和模板一旦改成更复杂的结构，运行时缓存一致性会更关键。当前实现里发送器实例会按通道缓存，若通道密钥、路由规则或 provider 配置被更新，没有额外机制时，线上可能继续使用旧实例。

建议文档补充：

- 通道更新后是否立即使缓存失效
- 模板更新后编译缓存如何清理
- 是否需要基于 `updatedAt` / `version` 做缓存键隔离

否则配置变更后“页面上已经保存成功，实际发送仍走旧配置”的问题很难排查。

### 10. 多来源数据合并优先级需要先定义

本文档里运行时至少会出现四类数据源：

- 模板 locale 文案
- 模板 `channels.<provider>` 片段
- 通道 `properties`
- 发送请求的 `params` / 动态 attributes

但目前没有定义冲突字段的覆盖优先级，也没有列出保留字段。例如 HTTP Email 的 `to`、`subject`、`content`，Slack 的 `channel`，Telegram 的 `chat_id`，FCM 的 `message.token` 都属于高风险字段，不能随意被模板变量或动态属性覆盖。

建议文档补一个“字段合并与保留关键字”小节，把覆盖顺序和禁止覆盖字段一次写死，避免开发阶段各 provider 自己做一套。
