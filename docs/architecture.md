# Ass Kicker 架构说明

## 概览

当前仓库采用前后端分离加双服务部署架构：

- `ui`：Vue 3 + Vite 管理后台
- `svr/manager`：系统配置、管理接口与 UI 托管，单实例部署
- `svr/worker`：发送任务接入、Kafka 生产消费与实际发送执行，支持水平扩展
- `svr/common`：共享领域模型、渠道实现、模板引擎、仓储与通用配置
- 基础设施：MongoDB + Kafka

## 模块职责

### `svr/common`

承载双服务共享代码：

- `model`：实体、枚举、状态定义
- `channel`：渠道抽象、渠道实现、工厂与管理器
- `repository`：MongoDB 访问层
- `service`：用户、模板、通道、记录、API Key 等公共服务
- `security`、`dto`、`exception`、`util`、公共配置
- `TemplateEngine`

### `svr/manager`

职责：

- 提供前端页面所需的全部管理接口
- 托管打包后的 UI 静态资源
- 提供登录、刷新令牌、权限控制与 OpenAPI 文档

主要接口：

- `/v1/auth/**`
- `/v1/users/**`
- `/v1/auth/apikeys/**`
- `/v1/channels/**`
- `/v1/templates/**`
- `/v1/records/**`

### `svr/worker`

职责：

- 暴露发送任务入口
- 将发送任务写入 Kafka
- 消费 Kafka 任务并执行模板渲染、渠道调度与发送
- 写入发送记录

主要接口：

- `/v1/send`
- `/v1/submit`

其中 `/v1/send` 与 `/v1/submit` 都只负责入队并返回 `taskId`。

## 数据与消息流

### 管理流

1. 浏览器访问 `manager` 提供的 UI 页面。
2. UI 调用 `manager` 的管理接口。
3. `manager` 读写 MongoDB 中的用户、模板、通道与记录数据。
4. `worker` 运行时直接从 MongoDB 读取最新模板和通道配置。

### 发送流

1. 调用方访问 `worker` 的 `/v1/send` 或 `/v1/submit`。
2. `worker` 校验请求并补全 `taskId`、`submittedAt`。
3. `worker` 将 `UniTask` 写入 Kafka。
4. `worker` 消费任务并调用 `TemplateEngine`。
5. `worker` 通过 `ChannelManager` 选择渠道并执行发送。
6. `worker` 将发送结果写入 `RecordEntity`。

## 构建与运行

本地启动：

```bash
mvn -f svr/manager/pom.xml spring-boot:run
mvn -f svr/worker/pom.xml spring-boot:run
npm --prefix ui run dev
```

聚合构建：

```bash
mvn -f svr/pom.xml -DskipTests package
```

默认端口：

- `manager`：`8080`，同时提供 UI 页面和管理 API
- `worker`：`8081`
- `ui` 开发服务：`5173`
