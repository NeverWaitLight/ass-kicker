# Ass Kicker

Ass Kicker 是一个前后端分离的消息发送平台：

- `ui`：Vue 3 + Vite 管理后台
- `svr/manager`：管理接口与 UI 托管服务
- `svr/worker`：发送接入、MQ 生产消费与发送执行服务
- 基础依赖：MongoDB、Kafka

## 项目结构

```text
.
├─ui              # 前端项目
├─svr             # 后端聚合工程
├─deploy          # Docker Compose 与部署模板
├─docs            # 补充文档
└─benchmark       # 压测脚本与记录
```

## 本地开发

### 后端

默认依赖：

- MongoDB：`mongodb://admin:123456@localhost:27017/asskicker?authSource=admin`
- Kafka：`localhost:9092`

启动管理端：

```bash
mvn -f svr/manager/pom.xml spring-boot:run
```

启动工作节点：

```bash
mvn -f svr/worker/pom.xml spring-boot:run
```

### 前端

前端开发模式通过 Vite 代理 `/v1` 到本地 `manager`。

```bash
npm --prefix ui install
npm --prefix ui run dev
```

默认访问地址：

- UI 开发服务：`http://localhost:5173`
- 管理端页面与接口：`http://localhost:8080`
- 管理端 API 文档：`http://localhost:8080/scalar`
- 发送端接口：`http://localhost:8081`

## Docker 部署

仓库提供以下部署文件：

- [deploy/docker-compose.yml](deploy/docker-compose.yml)
- [deploy/.env.example](deploy/.env.example)
- [svr/manager/Dockerfile](svr/manager/Dockerfile)
- [svr/worker/Dockerfile](svr/worker/Dockerfile)

启动步骤：

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
```

部署后访问：

```text
http://localhost:8080
```

容器拓扑：

- `manager`：Spring Boot 管理服务，同时托管打包后的 UI 页面
- `worker`：Spring Boot 发送服务
- `mongo`：业务数据存储
- `kafka`：发送任务异步队列

常用命令：

```bash
docker compose ps
docker compose logs -f manager
docker compose logs -f worker
docker compose down
docker compose down -v
```

## API 示例

```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'

curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'
```
