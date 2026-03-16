# Backend

本后端项目要实现的是基于 Spring 的全反应式链路。

## 技术栈

Spring Boot 3.2 WebFlux、Java 21、R2DBC（PostgreSQL）、Spring Security（JWT）、SpringDoc OpenAPI。Web 层为 RouterFunction + Handler 函数式端点，数据层与 HTTP 客户端均为非阻塞反应式。

## 模块与职责

**config** 应用与基础设施配置。R2dbcConfig 提供 R2DBC 自定义类型转换（如 ChannelType 与库表互转）；OpenApiConfig 配置 Swagger/OpenAPI；NativeRuntimeHints 为 GraalVM 原生镜像提供反射等运行时提示。

**router** 定义 HTTP 路由，将请求绑定到对应 Handler。包含 AuthRouter、ChannelRouter、HealthRouter、LanguageTemplateRouter、StatusRouter、TemplateRouter、UserRouter，均使用 WebFlux 的 RouterFunction 声明路由与 OpenAPI 注解。

**handlers** 处理具体请求，解析参数、调用 Service、构造 ServerResponse。AuthHandler 处理登录注册刷新令牌；ChannelHandler 处理渠道 CRUD 与测试发送；LanguageTemplateHandler、TemplateHandler 处理模板与多语言模板；UserHandler 处理用户管理。统一返回 Mono&lt;ServerResponse&gt;，与全链路反应式一致。

**service / service.impl** 业务逻辑层。AuthService 负责认证与 JWT；ChannelService 渠道的增删改查与配置，以及测试发送（按 type+properties 组装 ChannelConfig、调用 channels 包内 Factory 创建 Channel 并 send）；LanguageTemplateService、TemplateService 模板与多语言模板；UserService 用户与密码。

**repository** 数据访问层，基于 Spring Data R2DBC。SenderRepository、LanguageTemplateRepository、TemplateRepository、UserRepository 等提供反应式 CRUD 与查询；RegistrationLock、PostgresRegistrationLock 提供注册等场景的分布式锁。

**model** 领域实体与枚举。Channel、ChannelType、Template、LanguageTemplate、Language、User、UserStatus、UserRole 等，与 R2DBC 表结构对应。

**dto** 请求与响应 DTO。按子包划分：auth（登录、注册、刷新、Token 响应）、channel（如测试发送请求）、user（分页、创建、更新用户名、改密、重置密码等）。

**converter** 实体与 DTO 的转换，如 UserConverter，供 Handler/Service 层使用。

**channel** 消息发送抽象与多通道实现。顶层 Sender、SenderConfig、MessageRequest、MessageResponse；email 子包提供 EmailSender 接口及 Smtp、Http 实现，EmailSenderFactory、各类 EmailSenderConfig、EmailSenderPropertyMapper 负责配置与属性映射；im 子包提供 IMSender 及钉钉、企业微信实现，IMSenderFactory、IMSenderPropertyMapper、各 IMSenderConfig 负责 IM 渠道的装配与配置。发送逻辑可基于 WebClient 等实现，保持反应式。

**channel** 渠道侧配置与安全。ChannelCryptoConfig、ChannelCryptoProperties、ChannelPropertyCrypto 对渠道敏感配置（如密钥、Token）做加解密与存储，避免明文落库。

**security** 认证与授权。JwtService、JwtProperties、JwtPayload、JwtTokenType 负责 JWT 签发与解析；JwtServerAuthenticationConverter、JwtReactiveAuthenticationManager 将请求转为 Spring Security 的 Authentication；UserPrincipal 当前用户主体；SecurityConfig 配置需认证/放行的路径与反应式安全链。

**logging** 请求链路与日志上下文。RequestIdFilter 为请求注入或传递请求 ID；MdcContextLifterConfiguration 将 Reactor Context 中的信息放入 MDC，便于日志追踪。

### MongoDB

```sh
docker run -d --name mongodb -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=123456 mongo:latest
```

### Kafka

```sh
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

---

## 项目现状与关键缺口

本节记录当前实现状态与尚未完成的关键节点，便于排期与迭代。

### 当前已实现

| 层级                  | 内容                                                                                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 前端 (Vue3)           | 登录页、通道管理、模板管理、用户管理、发送记录列表/详情、个人设置、深色模式                                                           |
| 后端 (Spring WebFlux) | JWT 认证、用户 CRUD、通道 CRUD、模板 CRUD、Submit → Kafka → Send 全链路、Email/SMS/IM/Push 各渠道发送器、测试发送、Channel Debug 模拟 |
| 基础设施              | MongoDB、Kafka、Caffeine 缓存、异步日志、请求追踪 ID、GraalVM Native、CI 流水线、压测脚本                                             |

### 关键缺口（按优先级）

**P0 阻断**

- **无 Docker Compose**：强依赖 MongoDB 与 Kafka，根目录无 `docker-compose.yml`，本地与 CI 需手动起库和消息队列，影响开发与联调。

**P1 核心**

- **Submit API 鉴权方式**：`/api/submit` 仅支持 JWT 用户认证，缺少 API Key 机制，不适合外部业务系统以服务身份调用通知网关。
- **企业微信 IM 前端配置**：后端 `WechatWorkIMChannel` 已实现，前端通道配置表单（如 webhook URL）对企微的支持可能不完整，需对照钉钉 IM 配置补齐。

**P2 健壮性**

- **Kafka 消费失败无重试/DLQ**：`SendTaskConsumer` 消费失败直接写 FAILED，无死信队列或重试，网络或第三方临时故障无法自动补偿。

**P3 增强**

- **前端无「手动发送」入口**：无 Submit 页面，无法在控制台手动触发发送，仅适合纯 API 使用；若需演示或运营自测，可补发送页。
- **通道选择策略**：`ChannelManager.selectChannel()` 当前为随机选取，无权重、优先级或健康检查，多通道时无法精细控制选路。