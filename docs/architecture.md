# Ass Kicker 架构说明

## 概览

Ass Kicker 当前采用前后端分离 + 双后端服务拆分架构：

- `ui`
  - 基于 Vue 3 + Vite 的管理后台前端
  - 本地开发时独立运行
  - 生产环境构建产物由 `manager` 托管
- `svr/manager`
  - 系统配置与管理服务
  - 提供前端页面所需的全部管理接口
  - 单实例部署
- `svr/worker`
  - 发送任务接入与执行服务
  - 通过 `SendController` 接收发送请求
  - 通过 `SendTaskProducer` 写入 Kafka
  - 通过 `SendTaskConsumer` 消费 Kafka、调度渠道发送与记录结果
  - 支持多实例水平扩展
- `svr/common`
  - 管理端和工作节点共享的领域模型、仓储、模板引擎、渠道实现和公共配置

基础设施依赖：

- MongoDB：业务数据存储
- Kafka：发送任务异步队列

## 模块职责

### `svr/common`

`common` 承载双服务共享代码，主要包括：

- `model`
  - 用户、模板、通道、发送记录、API Key 等实体
- `channel`
  - 渠道接口、渠道实现、工厂与管理器
- `repository`
  - MongoDB 访问层
- `service`
  - 用户、模板、通道、记录、API Key 等公共服务
- `dto`、`exception`、`util`、`security`
  - 共享 DTO、异常、工具类与安全基础能力
- `TemplateEngine`
  - 模板渲染能力

### `svr/manager`

`manager` 的职责是系统配置和管理面能力，主要包括：

- 提供前端页面所需的全部接口
- 托管打包后的 `ui` 静态资源
- 提供登录、刷新令牌、权限校验和 API 文档

主要接口：

- `/v1/auth/**`
- `/v1/users/**`
- `/v1/auth/apikeys/**`
- `/v1/channels/**`
- `/v1/templates/**`
- `/v1/records/**`

### `svr/worker`

`worker` 的职责是发送链路接入和异步执行，主要包括：

- 对外暴露发送任务入口
- 将发送任务写入 Kafka
- 从 Kafka 消费任务
- 调用模板渲染与渠道调度逻辑执行发送
- 写入发送记录

当前代码实现里，任务接入和任务消费都位于 `svr/worker` 模块内：`SendController` 负责 `/v1/send`、`/v1/submit` 的 HTTP 接入和任务补全，`SendTaskProducer` 负责将 `UniTask` 投递到 Kafka，`SendTaskConsumer` 负责监听 Kafka 并触发真实发送。这意味着 `worker` 不是单纯的 MQ 消费进程，而是同时包含“接收任务并入队”和“消费任务并发送”两段链路。

主要接口：

- `POST /v1/send`
- `POST /v1/submit`

当前这两个接口都采用“入队并返回 `taskId`”语义，接口成功返回不表示消息已经发送完成，真正发送在 Kafka 消费端异步完成。

## 数据流与调用链路

### 管理链路

1. 浏览器访问 `manager` 提供的 UI 页面。
2. UI 调用 `manager` 的管理接口。
3. `manager` 读写 MongoDB 中的用户、模板、通道、API Key 和发送记录数据。
4. `worker` 在执行发送时从 MongoDB 读取最新模板和通道配置。

### 发送链路

1. 调用方访问 `worker` 的 `/v1/send` 或 `/v1/submit`。
2. `worker` 校验请求并补全 `taskId`、`submittedAt` 等字段。
3. `worker` 将 `UniTask` 写入 Kafka。
4. Kafka 消费端读取任务并调用 `TemplateEngine` 渲染模板。
5. `worker` 通过 `ChannelManager` 选择渠道并执行发送。
6. `worker` 将发送结果写入 `RecordEntity`。

## 部署模型

### 单机最小部署

最小可运行形态包括：

- `manager` × 1
- `worker` × 1
- `mongo` × 1
- `kafka` × 1

适合本地开发、测试和小规模私有部署。

### 生产部署建议

- `manager`
  - 单实例部署
  - 负责 UI 和管理 API
- `worker`
  - 多实例部署
  - 通过共享 Kafka Consumer Group 分摊发送任务
  - 可以按发送吞吐需求水平扩展

部署原则：

- `manager` 不承担发送吞吐扩容职责
- `worker` 不承担管理后台职责
- 二者共享 MongoDB 和 Kafka

## 构建与运行

### 本地启动

启动 `manager`：

```bash
mvn -f svr/manager/pom.xml spring-boot:run
```

启动 `worker`：

```bash
mvn -f svr/worker/pom.xml spring-boot:run
```

启动前端开发服务：

```bash
npm --prefix ui ci
npm --prefix ui run dev
```

### 聚合构建

```bash
mvn -f svr/pom.xml -DskipTests package
```

构建行为包括：

- 构建 `svr/common`
- 构建 `svr/manager`
- 在 `manager` 打包时联动执行 `ui` 的 `npm ci` 和 `npm run build`
- 将前端静态资源打入 `manager` jar
- 构建 `svr/worker`
- 生成 `manager` 与 `worker` 的发行压缩包

## 默认端口

- `manager`
  - `8080`
  - 同时提供 UI 页面和管理 API
- `worker`
  - `8081`
- `ui` 开发服务
  - `5173`

## 发布产物

`manager` 和 `worker` 都会在 `package` 阶段生成发行压缩包：

- `manager-<version>.tar.gz`
- `manager-<version>.zip`
- `worker-<version>.tar.gz`
- `worker-<version>.zip`

约定：

- `.tar.gz`
  - 用于 Linux / macOS
  - 包含 `.sh` 启动脚本
- `.zip`
  - 用于 Windows
  - 包含 `.bat` / `.cmd` 启动脚本

GitHub Actions 会在推送 `v*` tag 时自动构建并上传这些 release assets。

## 目录关系

```text
.
├─ ui/
├─ svr/
│  ├─ common/
│  ├─ manager/
│  └─ worker/
├─ deploy/
└─ docs/
```

如果需要快速了解项目入口，优先从这些位置开始：

- [`README.md`](../README.md)
- [`deploy/docker-compose.yml`](../deploy/docker-compose.yml)
- [`svr/manager/pom.xml`](../svr/manager/pom.xml)
- [`svr/worker/pom.xml`](../svr/worker/pom.xml)
