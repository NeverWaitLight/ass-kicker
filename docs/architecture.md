# Ass Kicker 架构说明

## 概览

当前仓库采用前后端分离 + 后端双服务部署架构：

- `frontend`：Vue 3 + Vite 管理后台
- `svr/manager`：系统配置与管理接口，单实例部署
- `svr/worker`：发送任务接入、MQ 生产消费、实际发送执行，支持水平扩展
- `svr/common`：共享领域模型、渠道实现、模板引擎、仓储、通用配置与工具
- 基础设施：MongoDB + Kafka

核心目标：

- 管理端职责单一，只服务前端页面和系统配置
- 发送链路与管理链路解耦，便于按吞吐单独扩容 `worker`
- 共享领域代码沉到 `common`，避免双服务重复维护

## 后端模块

### `svr/common`

承载双服务共享代码：

- `model`：实体、枚举、状态定义
- `channel`：渠道抽象、渠道实现、渠道工厂、渠道管理器
- `repository`：Mongo 访问层
- `service`：通道、模板、记录、用户、API Key 等共享服务
- `security`：JWT / API Key 基础组件
- `converter`、`dto`、`exception`、`util`、通用配置
- `TemplateEngine`

约束：

- 不放运行入口
- 不放仅某个服务独占的控制器
- 不直接表达部署角色，只表达共享能力

### `svr/manager`

职责：

- 提供前端页面所需的所有管理接口
- 管理用户、API Key、模板、通道、发送记录
- 提供登录、刷新令牌、权限控制
- 提供 OpenAPI / Scalar 文档

典型接口：

- `/v1/auth/**`
- `/v1/users/**`
- `/v1/auth/apikeys/**`
- `/v1/channels/**`
- `/v1/templates/**`
- `/v1/records/**`

部署策略：

- 单实例部署
- 不接入 Kafka 消费链路
- 不执行真正的业务发送

### `svr/worker`

职责：

- 暴露发送任务入口
- 将发送任务写入 Kafka
- 从 Kafka 消费发送任务
- 调用模板渲染、渠道选择与渠道发送逻辑
- 写入发送记录

典型接口：

- `/v1/send`
- `/v1/submit`

当前语义：

- `/v1/send` 与 `/v1/submit` 都只负责入队并返回 `taskId`
- 实际发送统一在 Kafka 消费链路执行

部署策略：

- 支持多实例水平扩展
- 共享同一 MongoDB 与 Kafka
- 使用同一 consumer group 分摊消费负载

## 数据与消息流

### 管理流

1. 前端调用 `manager` 管理接口
2. `manager` 读写 MongoDB 中的用户、模板、通道、记录等数据
3. `worker` 运行时直接从 MongoDB 读取最新模板与通道配置

### 发送流

1. 调用方访问 `worker` 的 `/v1/send` 或 `/v1/submit`
2. `worker` 校验并补全 `taskId`、`submittedAt`
3. `worker` 将 `UniTask` 写入 Kafka Topic
4. `worker` 消费任务并调用 `TemplateEngine`
5. `worker` 通过 `ChannelManager` 选择可用渠道
6. `worker` 执行具体渠道发送
7. `worker` 将成功或失败结果写入 `RecordEntity`

## 技术选型

- Java 21
- Spring Boot 3.2
- Spring WebFlux
- Reactive MongoDB
- Spring Security + JWT
- Spring Kafka
- Mustache
- MapStruct
- Caffeine

## 运行与构建

本地启动：

```bash
mvn -f svr/manager/pom.xml spring-boot:run
mvn -f svr/worker/pom.xml spring-boot:run
npm --prefix frontend run dev
```

聚合构建：

```bash
mvn -f svr/pom.xml -DskipTests package
```

Docker Compose：

- `manager` 默认端口 `8080`
- `worker` 默认端口 `8081`
- `frontend` 默认端口 `8088`

## 部署建议

- `manager` 保持单实例，便于统一管理与简化路由
- `worker` 根据发送吞吐扩容，实例数不应超过 Kafka 相关 topic 分区数太多
- `ASS_KICKER_SNOWFLAKE_WORKER_ID` 建议继续走 Mongo 注册分配，避免多实例 ID 冲突
- 前端只访问 `manager`
- 发送 API 调用方直接访问 `worker`
