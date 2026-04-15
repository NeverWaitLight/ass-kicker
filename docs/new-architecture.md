这是一个非常经典且核心的系统架构问题：**“如何用统一的内部模型，去适配千奇百怪的外部 API？”**

解决这个问题的最优雅方案是结合 **策略模式 (Strategy Pattern)**、**适配器模式 (Adapter Pattern)** 以及 **基于 JSON 的多态配置机制**。

核心思想是：**系统内部绝对不碰外部 API 的具体字段，所有转化都在“边缘（Handler）”完成。**

以下是落地这套架构的详细设计方案：

---

### 一、 架构概览

你需要将系统划分为三个完全解耦的层次：

1. **统一消息体 (Unified Message)：** 业务侧发出的消息，只包含纯粹的内容语义（如：标题、正文、跳转链接）。
2. **多态通道配置 (Polymorphic Config)：** 数据库中统一存储，但根据通道类型反序列化为不同的配置对象。
3. **策略处理器 (Channel Handler)：** 针对每个提供商（Provider）写一个专属的处理类，负责“拼装特有 Payload”并“发送 HTTP 请求”。

---

### 二、 通道配置 (Channel Config) 怎么做最优雅？

**千万不要**在数据库表里建 50 个字段（`telegram_token`, `ding_app_key`, `apns_p8` 等）。
**优雅的做法是：基础字段 + 动态 JSON 扩展字段。**

#### 1. 数据库表设计 (`channel_config` 表)

| 字段名         | 类型      | 说明                         | 示例                     |
| :------------- | :-------- | :--------------------------- | :----------------------- |
| `id`           | Long      | 主键                         | 1                        |
| `config_key`   | String    | 业务侧使用的唯一标识         | `my-tg-bot-01`           |
| `channel_type` | String    | 通道类型                     | `IM_BOT`                 |
| `provider`     | String    | 服务商                       | `TELEGRAM`               |
| `ext_props`    | JSON/Text | **核心！存放各平台专属配置** | `{"botToken":"123:ABC"}` |

#### 2. 代码实现：使用多态反序列化

在 Java 中，定义一个空的 Marker 接口或抽象类，各个平台实现自己的配置类。

```java
// 基础扩展配置接口
public interface ExtConfig {}

// Telegram 专属配置
@Data
public class TelegramExtConfig implements ExtConfig {
    private String botToken;
}

// 钉钉企业内部机器人配置
@Data
public class DingTalkBotExtConfig implements ExtConfig {
    private String appKey;
    private String appSecret;
    private Long agentId;
}

// APNS 推送配置
@Data
public class ApnsExtConfig implements ExtConfig {
    private String bundleId;
    private String p8CertContent;
    private String keyId;
    private String teamId;
}
```

_运行时，配置服务根据 `provider` 的值，将数据库里的 `ext_props` JSON 字符串反序列化为对应的具体类（可以使用 Jackson 的 `@JsonTypeInfo` 或手动 Factory）。_

---

### 三、 模板（Body 结构）怎么适配？

**不要试图用 Freemarker/Velocity 在数据库里存一堆复杂的 JSON 模板！** 外部 API 的 JSON 结构经常有嵌套、转义问题，用字符串模板引擎维护起来非常痛苦。

**最优雅的方式是：代码即模板（构建器模式）。**
业务侧只管发“统一消息”，具体的 Handler 负责将“统一消息”映射为第三方需要的特定结构。

#### 1. 定义业务侧的“统一消息” (UnifiedMessage)

不管底层发什么，业务层面通常只关心这几个要素：

```java
@Data
@Builder
public class UnifiedMessage {
    private String title;       // 标题（邮件、推送常用）
    private String content;     // 正文（文本或Markdown格式）
    private String url;         // 点击跳转链接（推送、卡片消息常用）
    private Map<String, Object> extraData; // 留一个口子传特殊业务参数
}
```

#### 2. 定义统一处理接口 (Handler 策略接口)

```java
public interface ChannelHandler {

    /**
     * 该 Handler 是否支持处理此类型的地址
     */
    boolean supports(ChannelType type, String provider);

    /**
     * 核心发送逻辑
     * @param address 之前优化好的地址对象（含 target / senderKey）
     * @param message 统一消息体
     * @param extConfig 从数据库查出来的第三方专属配置
     */
    void send(Address address, UnifiedMessage message, ExtConfig extConfig);
}
```

#### 3. 实现具体的 Handler（在这里拼装特定的 Body）

**示例 A：Telegram Bot 发送器**

```java
@Component
public class TelegramBotHandler implements ChannelHandler {

    @Override
    public boolean supports(ChannelType type, String provider) {
        return type == ChannelType.IM_BOT && "TELEGRAM".equals(provider);
    }

    @Override
    public void send(Address address, UnifiedMessage message, ExtConfig config) {
        // 1. 强转配置类
        TelegramExtConfig tgConfig = (TelegramExtConfig) config;

        // 2. 拼装 Telegram 特有的 JSON Body结构 (可以直接用 Map 或定义 TgRequest POJO)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", address.getTarget());
        // Telegram 只需要把 title 和 content 拼在一起作为 text
        requestBody.put("text", "<b>" + message.getTitle() + "</b>\n" + message.getContent());
        requestBody.put("parse_mode", "HTML");

        // 3. 发起 HTTP 请求
        String apiUrl = "https://api.telegram.org/bot" + tgConfig.getBotToken() + "/sendMessage";
        httpClient.post(apiUrl, requestBody);
    }
}
```

**示例 B：钉钉群 Webhook 发送器**

```java
@Component
public class DingTalkWebhookHandler implements ChannelHandler {

    @Override
    public boolean supports(ChannelType type, String provider) {
        return type == ChannelType.IM_WEBHOOK && "DINGTALK".equals(provider);
    }

    @Override
    public void send(Address address, UnifiedMessage message, ExtConfig config) {
        // 注意：Webhook 场景 config 可能为空，因为鉴权信息全在 URL 里 (也就是 address.getTarget())

        // 2. 拼装钉钉特有的 Markdown JSON 结构
        Map<String, Object> markdownNode = new HashMap<>();
        markdownNode.put("title", message.getTitle());
        markdownNode.put("text", "### " + message.getTitle() + "\n" + message.getContent());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msgtype", "markdown");
        requestBody.put("markdown", markdownNode);

        // 3. 发起 HTTP 请求
        // 此时 address.getTarget() 就是完整的 Webhook URL
        httpClient.post(address.getTarget(), requestBody);
    }
}
```

**示例 C：FCM Push 推送器**

```java
@Component
public class FcmPushHandler implements ChannelHandler {

    // FCM 的 Payload 结构完全不同，包含 notification 和 data 两个节点
    @Override
    public void send(Address address, UnifiedMessage message, ExtConfig config) {
        FcmExtConfig fcmConfig = (FcmExtConfig) config;

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", message.getTitle());
        notification.put("body", message.getContent());

        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("token", address.getTarget()); // 设备 Token
        messageBody.put("notification", notification);
        messageBody.put("data", message.getExtraData()); // 透传业务数据

        // 发起 FCM 专属的鉴权请求...
    }
}
```

---

### 四、 核心调度引擎 (Router / Dispatcher)

最后，你需要一个统一的门面（Facade），将前面所有的东西串联起来。业务侧只需调用这一个类。

```java
@Service
@RequiredArgsConstructor
public class MessageDispatcher {

    // Spring 会自动注入所有的 Handler 实现类
    private final List<ChannelHandler> handlers;
    private final ConfigRepository configRepository; // 查配置的 DAO

    public void dispatch(Address address, UnifiedMessage message) {

        // 1. 查找对应的 Handler
        ChannelHandler matchedHandler = handlers.stream()
            .filter(h -> h.supports(address.getChannelType(), address.getChannelKey()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No handler found!"));

        // 2. 只有当不是 Webhook 时，才需要通过 senderKey 去查配置
        ExtConfig extConfig = null;
        if (StringUtils.isNotBlank(address.getSenderKey())) {
            String jsonProps = configRepository.getExtPropsByKey(address.getSenderKey());
            // 根据 provider 反序列化为具体的 Config 对象
            extConfig = ConfigParser.parse(jsonProps, address.getChannelKey());
        }

        // 3. 委派给具体的 Handler 执行
        try {
            matchedHandler.send(address, message, extConfig);
        } catch (Exception e) {
            // 统一处理重试逻辑、记录发送失败日志等
        }
    }
}
```

### 总结

这套设计的优雅之处在于：

1. **彻底符合开闭原则 (OCP)：** 明天产品经理说要接入“飞书机器人”，你只需要新增一个 `FeishuBotExtConfig` 和一个 `FeishuBotHandler`，**核心调度代码和现有的业务逻辑一行都不用改**。
2. **无需维护复杂的 JSON 模板：** 第三方 API 的数据结构由对应的 Java Handler 内聚管理，编译期就能检查语法错误，重构极度安全。
3. **配置隔离：** 不同的通道只关心它自己必需的字段，数据库只存通用的 JSON 字符串，灵活性极高。
