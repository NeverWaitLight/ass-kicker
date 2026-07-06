# ass-kicker Java 后端服务

基于 Spring Boot WebFlux 的消息推送服务后端。

## 快速开始

### 前置依赖

- Java 17+
- Maven 3.6+
- MongoDB
- RocketMQ

### 启动依赖服务

```bash
# 启动 MongoDB
docker compose -f ../../infra/docker/docker-compose-mongodb.yml up -d

# 启动 RocketMQ
docker compose -f ../../infra/docker/docker-compose-rocketmq.yml up -d
```

### 启动应用

```bash
# 使用 Maven
mvn spring-boot:run

# 或使用 IDE 运行
# 直接运行 com.github.waitlight.asskicker.AssKickerApplication
```

应用默认运行在 **8080** 端口。

## 接口文档

### 访问地址

启动应用后，可通过以下地址访问接口文档：

1. **Scalar UI** (推荐，现代化界面):
   ```
   http://localhost:8080/scalar
   ```

2. **OpenAPI JSON 规范**:
   ```
   http://localhost:8080/v3/api-docs
   ```

### API 认证

接口支持两种认证方式：

- **JWT Bearer Token**: 
  ```
  Authorization: Bearer <access_token>
  ```

- **API Key** (可与 JWT 二选一，用于 `/v1/send`):
  ```
  X-API-Key: <api-key>
  ```

## 配置说明

主要配置项通过环境变量设置：

| 环境变量                           | 默认值                                                            | 说明                         |
| ---------------------------------- | ----------------------------------------------------------------- | ---------------------------- |
| `SERVER_PORT`                      | 8080                                                              | 服务端口                     |
| `SPRING_DATA_MONGODB_URI`          | mongodb://admin:123456@localhost:27017/asskicker?authSource=admin | MongoDB 连接 URI             |
| `ROCKETMQ_NAME_SERVER`             | localhost:9876                                                    | RocketMQ NameServer 地址     |
| `ASS_KICKER_SECURITY_JWT_SECRET`   | change-this-secret-to-32-chars-minimum                            | JWT 密钥（生产环境必须修改） |
| `ASS_KICKER_CHANNEL_DEBUG_ENABLED` | true                                                              | 渠道调试模式开关             |
| `SCALAR_ENABLED`                   | true                                                              | Scalar 文档界面开关          |

完整配置请参考 `src/main/resources/application.yml`。

## 项目结构

```
src/main/java/com/github/waitlight/asskicker/
├── channel/          # 各类消息推送渠道实现
├── config/           # 配置类
├── controller/       # REST API 控制器
├── converter/        # DTO 与实体转换器
├── dto/              # 数据传输对象
├── exception/        # 异常处理
├── faced/            # 对外接口
├── model/            # 数据模型
└── ...
```

## 开发

### 构建项目

```bash
mvn clean package
```

### 运行测试

```bash
mvn test
```

## 技术栈

- Spring Boot 3.x (WebFlux)
- MongoDB (响应式驱动)
- RocketMQ
- SpringDoc OpenAPI (Scalar UI)
- JWT 认证
- Caffeine Cache
