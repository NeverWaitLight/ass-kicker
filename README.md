<h1>
  <img src="web/src/assets/logo.png" alt="Ass Kicker Logo" width="56" style="vertical-align: middle;" />
  <span style="vertical-align: middle;">Ass Kicker</span>
</h1>

Ass Kicker 是一个面向多渠道消息发送场景的开源消息平台。

它提供一个用于系统配置和可视化管理的 `manager` 服务，以及一个可水平扩展的 `worker` 服务，用于接收发送任务、写入消息队列、消费任务并执行真实发送。当前仓库同时包含前端管理界面 `web`、后端工程 `services/java` 和 Docker 部署配置，适合本地开发、私有部署和二次开发。

## Features

- 多模块架构：`manager` 负责管理和 UI 托管，`worker` 负责发送链路，便于独立部署和扩容
- 多渠道支持：内置短信、邮件、Webhook、Bot Push 等多种消息渠道
- 模板化发送：支持模板、变量渲染和渠道调度
- 异步处理：基于 RocketMQ 解耦任务提交与消息发送
- 可视化管理：提供基于 Vue 3 的管理后台
- 私有化部署：提供 Docker Compose、模块发行包和 GitHub Actions 构建链路

## Supported Channels

当前仓库内置的渠道实现位于 [`services/java/src/main/java/com/github/waitlight/asskicker/channel`](services/java/src/main/java/com/github/waitlight/asskicker/channel)，包括：

- Aliyun SMS
- AWS SNS SMS
- SMTP Email
- APNs
- FCM
- 钉钉机器人 / Webhook
- 飞书机器人 / Webhook
- 企业微信机器人 / Webhook

## Architecture

项目当前采用前后端分离 + 双服务部署架构：

- `web`
  - Vue 3 + Vite 管理后台
  - 本地开发时可独立运行
  - 生产环境下由 `manager` 打包并托管
- `services/java`
  - Spring Boot 后端工程
  - 包含 `manager`（管理服务）和 `worker`（发送服务）两个模块
  - `manager` 提供用户、API Key、模板、渠道、发送记录等管理接口，托管打包后的前端页面
  - `worker` 负责发送链路，接收发送请求、写入消息队列、消费任务并执行真实发送

基础依赖：

- MongoDB：业务数据存储
- RocketMQ：发送任务异步解耦

## Project Layout

```text
.
├─ web/                      # 前端管理界面
├─ services/
│  ├─ java/                  # 后端 Java 工程 (Spring Boot)
│  └─ go/                    # 后端 Go 工程 (TODO)
├─ infra/
│  ├─ docker/                # Docker Compose 与环境变量模板
│  └─ scripts/               # 运维/调试脚本
├─ testing/
│  └─ benchmark/             # 压测脚本
├─ docs/                     # 架构与设计文档
├─ Makefile                  # 顶层构建入口
└─ REPO-STRUCTURE.md         # 目录规划说明
```

## Quick Start

### Requirements

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+
- MongoDB 7+
- RocketMQ 5+

说明：

- `services/java` 打包时会直接调用本机 `npm ci` 和 `npm run build`
- 如果你在本地执行 `mvn clean package`，机器上必须已经安装可用的 `node` 和 `npm`

### 1. Start Dependencies

如果你本地已有 MongoDB 和 RocketMQ，可以直接跳过这一步。否则可以使用仓库里的 Docker Compose：

```bash
make deps-mongo
make deps-rocketmq
```

### 2. Run Backend Services

启动后端：

```bash
make run-java
```

默认端口：

- `manager`: `http://localhost:8080`
- `manager` API 文档：`http://localhost:8080/scalar`

### 3. Run UI in Development Mode

本地前端开发时，`web` 通过 Vite 代理 `/v1` 到本地 `manager`。

```bash
make run-web
```

默认访问地址：

- UI 开发服务：`http://localhost:5173`

一键启动前后端调试：

```bash
bash infra/scripts/debug.sh
```

## Usage

### Manager APIs

`manager` 负责系统配置和管理后台所需接口，主要包括：

- `/v1/auth/**`
- `/v1/users/**`
- `/v1/auth/apikeys/**`
- `/v1/channels/**`
- `/v1/templates/**`
- `/v1/records/**`

### Worker APIs

`worker` 负责发送链路，主要入口包括：

- `POST /v1/send`
- `POST /v1/submit`

当前代码实现中，`worker` 同时承担任务接入和任务执行两类职责：`SendController` 接收发送请求并完成校验、补全 `taskId` 和 `submittedAt`，随后调用 `SendTaskProducer` 将 `UniTask` 写入 Kafka；同一个 `worker` 服务内的 `SendTaskConsumer` 再从 Kafka 消费任务并调用发送链路执行真实发送。

因此，`/v1/send` 和 `/v1/submit` 都采用“入队返回 `taskId`”语义，接口返回成功只表示任务已提交到 Kafka，真正的发送由 Kafka 消费端异步完成。

### Example

注册用户：

```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'
```

登录获取 token：

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'
```

## Build

### Build All

```bash
make build
```

这会：

- 构建 `services/java` 后端工程
- 在打包期间联动执行 `web` 的 `npm ci` 和 `npm run build`
- 将前端静态资源打入发行包
- 生成发行压缩包

### Build Separately

```bash
make build-java
make build-web
```

## Distribution Packages

`manager` 和 `worker` 在 `package` 阶段都会生成平台发行包：

- `manager-<version>.tar.gz`
- `manager-<version>.zip`
- `worker-<version>.tar.gz`
- `worker-<version>.zip`

约定如下：

- `.tar.gz`：用于 Linux / macOS，包含 `.sh` 启动脚本
- `.zip`：用于 Windows，包含 `.bat` / `.cmd` 启动脚本

GitHub Actions 在推送 `v*` tag 时会自动构建这些产物，并上传到对应 Release 的 assets。

## Docker Deployment

仓库已经提供完整部署文件：

- [`infra/docker/docker-compose-mongodb.yml`](infra/docker/docker-compose-mongodb.yml)
- [`infra/docker/docker-compose-rocketmq.yml`](infra/docker/docker-compose-rocketmq.yml)

启动依赖：

```bash
make deps-mongo
make deps-rocketmq
```

默认容器拓扑：

- `mongo`: 数据库
- `rocketmq`: 消息队列

如果需要提升发送吞吐，可以水平扩展 `worker`。

## Development Notes

- `manager` 建议单实例部署
- `worker` 设计为多实例水平扩展
- `worker` 依赖统一的 RocketMQ Consumer Group 共享消费分区
- `manager` 和 `worker` 共享 MongoDB 中的业务配置数据
- 本地测试时如果不希望真实发信，可通过配置关闭真实渠道或启用调试模式

## Roadmap

- 补充更多发送渠道和模板能力
- 补充更完整的监控、限流和重试策略
- 完善更细粒度的权限控制与操作审计
- 补充更系统的集成测试和部署示例

## Contributing

欢迎提交 Issue 和 Pull Request。

在提交代码前，建议至少完成以下检查：

```bash
make test
```

如果只修改了某一端：

```bash
make test-java
make test-web
```

## License

本项目基于 [LICENSE](LICENSE) 中定义的许可协议发布。
