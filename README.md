<h1>
  <img src="ui/src/assets/logo.png" alt="Ass Kicker Logo" width="56" style="vertical-align: middle;" />
  <span style="vertical-align: middle;">Ass Kicker</span>
</h1>

Ass Kicker 是一个面向多渠道消息发送场景的开源消息平台。

它提供一个用于系统配置和可视化管理的 `manager` 服务，以及一个可水平扩展的 `worker` 服务，用于接收发送任务、写入消息队列、消费任务并执行真实发送。当前仓库同时包含前端管理界面 `ui`、后端聚合工程 `svr` 和 Docker 部署配置，适合本地开发、私有部署和二次开发。

## Features

- 多模块架构：`manager` 负责管理和 UI 托管，`worker` 负责发送链路，便于独立部署和扩容
- 多渠道支持：内置短信、邮件、Webhook、Bot Push 等多种消息渠道
- 模板化发送：支持模板、变量渲染和渠道调度
- 异步处理：基于 Kafka 解耦任务提交与消息发送
- 可视化管理：提供基于 Vue 3 的管理后台
- 私有化部署：提供 Docker Compose、模块发行包和 GitHub Actions 构建链路

## Supported Channels

当前仓库内置的渠道实现位于 [`svr/common/src/main/java/com/github/waitlight/asskicker/channel`](svr/common/src/main/java/com/github/waitlight/asskicker/channel)，包括：

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

- `ui`
  - Vue 3 + Vite 管理后台
  - 本地开发时可独立运行
  - 生产环境下由 `manager` 打包并托管
- `svr/manager`
  - 管理端服务
  - 提供用户、API Key、模板、渠道、发送记录等管理接口
  - 提供登录鉴权和 API 文档
  - 托管打包后的前端页面
- `svr/worker`
  - 发送执行服务
  - 接收发送请求，写入 Kafka
  - 从 Kafka 消费任务，执行模板渲染、渠道发送和发送记录写入
  - 支持多实例水平扩展
- `svr/common`
  - 共享实体、枚举、仓储、模板引擎、渠道实现和公共配置

基础依赖：

- MongoDB：业务数据存储
- Kafka：发送任务异步解耦

## Project Layout

```text
.
├─ ui/                  # 前端管理界面
├─ svr/                 # 后端 Maven 聚合工程
│  ├─ common/           # 共享领域模型、仓储、模板引擎、渠道实现
│  ├─ manager/          # 管理服务，打包并托管 UI
│  └─ worker/           # 发送服务，负责 MQ 与发送执行
├─ deploy/              # Docker Compose 与环境变量模板
├─ docs/                # 架构与补充文档
└─ benchmark/           # 压测脚本
```

## Quick Start

### Requirements

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+
- MongoDB 7+
- Kafka 3+

说明：

- `svr/manager` 打包时会直接调用本机 `npm ci` 和 `npm run build`
- 如果你在本地执行 `mvn clean package`，机器上必须已经安装可用的 `node` 和 `npm`

### 1. Start Dependencies

如果你本地已有 MongoDB 和 Kafka，可以直接跳过这一步。否则可以使用仓库里的 Docker Compose：

```bash
cd deploy
cp .env.example .env
docker compose up -d mongo kafka
```

默认依赖配置：

- MongoDB: `mongodb://admin:123456@localhost:27017/asskicker?authSource=admin`
- Kafka: `localhost:9092`

### 2. Run Backend Services

启动 `manager`：

```bash
mvn -f svr/manager/pom.xml spring-boot:run
```

启动 `worker`：

```bash
mvn -f svr/worker/pom.xml spring-boot:run
```

默认端口：

- `manager`: `http://localhost:8080`
- `worker`: `http://localhost:8081`
- `manager` API 文档：`http://localhost:8080/scalar`

### 3. Run UI in Development Mode

本地前端开发时，`ui` 通过 Vite 代理 `/v1` 到本地 `manager`。

```bash
npm --prefix ui ci
npm --prefix ui run dev
```

默认访问地址：

- UI 开发服务：`http://localhost:5173`

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

当前两者都采用“入队返回 `taskId`”语义，真正的发送由 Kafka 消费端异步执行。

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

### Build All Server Modules

```bash
mvn -f svr/pom.xml clean package
```

这会：

- 构建 `svr/common`
- 构建 `svr/manager`
- 在 `manager` 打包期间联动执行 `ui` 的 `npm ci` 和 `npm run build`
- 将前端静态资源打入 `manager` jar
- 构建 `svr/worker`
- 生成 `manager` 和 `worker` 的发行压缩包

### Build a Single Module

在模块目录内可以直接执行：

```bash
cd svr/manager
mvn clean package
```

或：

```bash
cd svr/worker
mvn clean package
```

仓库已经为这两个模块加了 `.mvn/maven.config`，会自动带上父工程和依赖模块构建参数。

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

- [`deploy/docker-compose.yml`](deploy/docker-compose.yml)
- [`deploy/.env.example`](deploy/.env.example)
- [`svr/manager/Dockerfile`](svr/manager/Dockerfile)
- [`svr/worker/Dockerfile`](svr/worker/Dockerfile)

启动方式：

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
```

部署完成后访问：

```text
http://localhost:8080
```

默认容器拓扑：

- `manager`: 管理服务和 UI 托管
- `worker`: 发送服务
- `mongo`: 数据库
- `kafka`: 消息队列

如果需要提升发送吞吐，可以水平扩展 `worker`。

## Development Notes

- `manager` 建议单实例部署
- `worker` 设计为多实例水平扩展
- `worker` 依赖统一的 Kafka Consumer Group 共享消费分区
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
mvn -f svr/pom.xml test
npm --prefix ui run build
```

如果修改了 `manager` 相关功能，也建议验证：

```bash
cd svr/manager
mvn clean package
```

## License

本项目基于 [LICENSE](LICENSE) 中定义的许可协议发布。
