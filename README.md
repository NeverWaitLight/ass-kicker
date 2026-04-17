# Ass Kicker

Ass Kicker 是一个前后端分离的消息发送平台：

- `frontend`：Vue 3 + Vite 管理后台
- `backend`：Spring Boot 3 + WebFlux 后端
- 基础依赖：MongoDB、Kafka

## 项目结构

```text
.
├─frontend        # 前端项目
├─backend         # 后端项目
├─deploy          # Docker Compose 与部署模板
├─docs            # 补充文档
└─benchmark       # 压测脚本与记录
```

## 本地开发

### 后端

后端默认依赖：

- MongoDB：`mongodb://admin:123456@localhost:27017/asskicker?authSource=admin`
- Kafka：`localhost:9092`

在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

### 前端

前端开发模式通过 Vite 代理 `/v1` 到本地后端。

在 `frontend` 目录运行：

```bash
npm install
npm run dev
```

默认访问地址：

- 前端：`http://localhost:5173`
- 后端接口：`http://localhost:8080`
- API 文档：`http://localhost:8080/scalar`

## Docker 部署

仓库已提供完整容器化部署文件：

- [deploy/docker-compose.yml](deploy/docker-compose.yml)
- [deploy/.env.example](deploy/.env.example)
- [backend/Dockerfile](backend/Dockerfile)
- [frontend/Dockerfile](frontend/Dockerfile)

### 启动步骤

1. 复制环境变量模板：

```bash
cd deploy
cp .env.example .env
```

2. 修改 `.env` 中的密码和 `JWT_SECRET`。

3. 启动服务：

```bash
docker compose up -d --build
```

4. 访问前端：

```text
http://localhost:8088
```

### 容器拓扑

- `frontend`：Nginx 托管前端静态资源，并将 `/v1` 反代到后端
- `backend`：Spring Boot 应用
- `mongo`：业务数据存储
- `kafka`：发送任务异步队列

### 常用命令

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

停止服务：

```bash
docker compose down
```

停止并删除数据卷：

```bash
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
