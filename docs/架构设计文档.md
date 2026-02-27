# Ass Kicker 架构设计文档

## 1. 系统概述

### 1.1 背景与目标

本项目用于建立现代化全栈开发基础架构，提供可运行的 Java 21 + Spring WebFlux 后端与 Vue 3 + Ant Design Vue 前端，帮助团队快速启动新项目开发。

### 1.2 技术栈总览

| 层级 | 技术选型 |
|------|----------|
| 后端 | Java 21、Spring Boot 3.x、Spring WebFlux |
| 前端 | Vue 3、Vite、Ant Design Vue |
| 构建 | Maven (后端)、npm/Vite (前端) |
| 原生镜像 | GraalVM Native Image |

---

## 2. 后端架构

### 2.1 核心架构

- **入口类**: `com.github.waitlight.asskicker.Application`
- **运行模型**: 响应式 WebFlux RouterFunction 模式
- **构建工具**: Maven

### 2.2 目录结构

```
backend/
├── pom.xml                                    # 依赖与构建配置
├── src/main/java/com/github/waitlight/asskicker/
│   ├── Application.java                       # 入口类
│   ├── handler/                               # 请求处理器
│   ├── router/                                # 路由定义
│   ├── service/                               # 业务逻辑
│   ├── repository/                            # 数据访问
│   ├── entity/                                # 实体模型
│   ├── dto/                                   # 数据传输对象
│   ├── mapper/                                # MapStruct 映射器
│   └── config/                                # 配置类
└── src/main/resources/
    ├── application.yml                        # 应用配置
    └── logback-spring.xml                     # 日志配置
```

### 2.3 Router/Handler 模式

系统采用 Spring WebFlux 的 Router/Handler 函数式编程模型，替代传统的 Controller 注解驱动模式。

#### 2.3.1 设计原理

| 特性 | Controller 模式 | Router/Handler 模式 |
|------|----------------|--------------------|
| 编程范式 | 注解驱动 | 函数式编程 |
| 并发模型 | 阻塞式 | 非阻塞式 |
| 性能 | 适用于一般场景 | 高并发场景更优 |
| 代码组织 | 类和方法 | 函数式组合 |

**优势**:
- 响应式编程：更好地支持异步、非阻塞操作
- 函数式编程：提供更简洁、可组合的 API 定义方式
- 性能优化：在高并发场景下有更好的性能表现
- 清晰分离：路由定义与业务逻辑分离，职责更加明确

#### 2.3.2 Handler 组件

Handler 负责实现具体的业务逻辑，遵循以下原则：
- 单一职责：每个 Handler 专注于特定领域的业务逻辑
- 无状态：Handler 应该是无状态的，依赖注入的服务来处理状态
- 响应式：所有操作都应该返回 Mono 或 Flux
- 错误处理：为每个操作提供适当的错误处理

```java
@Component
public class TemplateHandler implements BaseHandler {

    private final TemplateService templateService;

    public TemplateHandler(TemplateService templateService) {
        this.templateService = templateService;
    }

    public Mono<ServerResponse> createTemplate(ServerRequest request) {
        return request.bodyToMono(Template.class)
                .flatMap(template -> templateService.createTemplate(template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .onErrorResume(throwable -> 
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to create template")));
    }
}
```

#### 2.3.3 Router 组件

Router 定义 URL 路径与 Handler 方法之间的映射关系：

```java
@Configuration
public class TemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> templateRoutes(TemplateHandler templateHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), 
                       templateHandler::createTemplate)
                .andRoute(RequestPredicates.GET("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), 
                         templateHandler::getTemplateById)
                .andRoute(RequestPredicates.PUT("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), 
                         templateHandler::updateTemplate)
                .andRoute(RequestPredicates.DELETE("/api/templates/{id}"), 
                         templateHandler::deleteTemplate);
    }
}
```

### 2.4 响应式数据流

所有 Handler 方法都返回 `Mono<ServerResponse>` 或 `Flux<ServerResponse>`：

```java
public Mono<ServerResponse> getTemplateById(ServerRequest request) {
    String id = request.pathVariable("id");
    return templateService.getTemplateById(id)
            .flatMap(template -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(template)))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(throwable -> 
                ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BodyInserters.fromValue("Failed to retrieve template")));
}
```

### 2.5 错误处理

Router/Handler 模式提供统一的错误处理机制：
- `switchIfEmpty()`: 处理空结果情况
- `onErrorResume()`: 处理异常情况
- 自定义错误响应

---

## 3. 前端架构

### 3.1 技术栈

- **框架**: Vue 3
- **构建工具**: Vite
- **UI 库**: Ant Design Vue

### 3.2 目录结构

```
frontend/
├── package.json                    # 依赖与脚本
├── vite.config.js                  # Vite 配置
├── src/
│   ├── main.js                     # 应用入口
│   ├── App.vue                     # 应用主布局
│   ├── components/
│   │   └── Home.vue                # 首页组件
│   └── styles.css                  # 全局样式
```

### 3.3 UI 结构

使用 Ant Design Vue 的布局、卡片、排版等组件构建用户界面。

---

## 4. 对象映射规范 (MapStruct)

### 4.1 依赖配置

后端使用 Maven 管理 MapStruct 依赖：
- 运行时依赖：`org.mapstruct:mapstruct`
- 注解处理器：`org.mapstruct:mapstruct-processor`
- Lombok 兼容绑定：`org.projectlombok:lombok-mapstruct-binding`

### 4.2 Mapper 接口定义

在 `backend/src/main/java/com/github/waitlight/asskicker/mapper` 下创建接口，使用 `@Mapper(componentModel = "spring")` 标注以便 Spring 自动注入：

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserView toView(User user);

    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserView view);

    @AfterMapping
    default void clearPasswordHash(UserView view, @MappingTarget User user) {
        if (user != null) {
            user.setPasswordHash(null);
        }
    }
}
```

### 4.3 使用方式

通过构造器注入使用：

```java
private final UserMapper userMapper;

public UserServiceImpl(..., UserMapper userMapper) {
    this.userMapper = userMapper;
}

return userRepository.save(user).map(userMapper::toView);
```

### 4.4 自定义映射建议

- 对于 DTO 中缺失的敏感字段（如 `passwordHash`），使用 `@Mapping(ignore = true)` + `@AfterMapping` 进行显式处理
- 若字段名称不一致或需要转换逻辑，可使用 `@Mapping` 搭配 `@Named` 或 `@AfterMapping` 实现

---

## 5. 日志与请求追踪

### 5.1 异步日志配置

后端使用 Logback + `AsyncAppender` 实现异步日志，配置文件位于 `backend/src/main/resources/logback-spring.xml`。

**关键参数**:
- `logging.async.queue-size`: 异步队列大小，默认 `1024`
- `logging.async.discarding-threshold`: 丢弃阈值，默认 `0`（不丢弃）

日志输出使用 `STDOUT`，并通过 `ASYNC_STDOUT` 异步包装，适合容器化运行环境。

### 5.2 请求追踪 ID

系统为每个请求生成或复用 `X-Request-Id`：
- 如果请求头已携带 `X-Request-Id`，直接复用（便于跨服务传递）
- 如果缺失，则生成新的 UUID
- 响应头会回写相同的 `X-Request-Id`

请求 ID 会写入 MDC（key 为 `requestId`），并在日志格式中输出：
- 日志格式包含 `%X{requestId:-}`，确保所有日志可追踪到请求 ID

### 5.3 依赖说明

- `com.lmax:disruptor`: 高性能队列组件，为异步日志的高吞吐场景提供支撑
- MDC 支持来自 Spring Boot 默认日志栈（SLF4J + Logback），无需额外引入

---

## 6. 编程规范

### 6.1 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 变量/函数 | 驼峰命名法 (camelCase) | `userName`, `calculateTotal()` |
| 类/构造函数 | 帕斯卡命名法 (PascalCase) | `UserService`, `TemplateHandler` |
| 常量 | 大写字母 + 下划线 (UPPER_SNAKE_CASE) | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |

### 6.2 注释要求

- 函数和方法应包含 Javadoc-style 注释，说明参数和返回值
- 复杂逻辑应添加内联注释解释意图
- 类和模块应有简要描述其职责的注释

### 6.3 代码组织结构

**后端结构**:
- `router`: 路由定义
- `handler`: 请求处理
- `service`: 业务逻辑
- `repository`: 数据访问
- `entity`: 数据模型
- `dto`: 数据传输对象
- `mapper`: 对象映射

**前端结构**:
- `components`: 可复用 UI 组件
- `views/pages`: 页面组件
- `services/api`: API 调用
- `utils`: 工具函数
- `store`: 状态管理（如使用）

---

## 7. 时间字段处理规范

### 7.1 后端存储与传输

- 所有时间字段在后端存储和 HTTP 传输时必须使用 **13 位 UTC 时间戳**（毫秒级）
- 时间戳应为整数类型（Long 型），表示自 1970 年 1 月 1 日 00:00:00 UTC 以来的毫秒数
- 数据库中时间字段也应存储为 BIGINT 类型以支持 13 位时间戳

### 7.2 系统时间字段处理

`createdAt`、`updatedAt`、`lastLoginAt` 等系统时间字段：
- 全部由后端自动获取和维护
- 前端在创建或更新资源时，**不得传递**这些字段给后端
- 后端在接收请求时，**不能接收和使用**从前端传来的这些字段值，必须由后端生成
- 创建资源时：后端设置 `createdAt` 和 `updatedAt` 为当前时间
- 更新资源时：后端仅更新 `updatedAt` 为当前时间
- 用户登录时：后端更新 `lastLoginAt` 为当前时间

### 7.3 前端展示

- 前端仅在展示时间信息时，将 13 位 UTC 时间戳转换为本地时间
- 转换后的格式应符合人类习惯（如：YYYY-MM-DD HH:mm:ss）
- 所有时间计算和传输过程保持使用 UTC 时间戳

### 7.4 示例

**前端请求（不包含系统时间字段）**:
```javascript
fetch('/api/users', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'example',
    password: 'password'
    // 注意：不包含 createdAt、updatedAt、lastLoginAt 等字段
  })
});
```

**前端展示转换**:
```javascript
const response = await fetch('/api/users');
const users = await response.json();
users.forEach(user => {
  const createTime = new Date(user.createdAt).toLocaleString();
  const updateTime = new Date(user.updatedAt).toLocaleString();
});
```

**后端处理**:
```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
    User user = new User();
    long now = System.currentTimeMillis();

    // 设置用户提供的字段
    user.setUsername(request.getUsername());
    user.setPasswordHash(request.getPassword());

    // 系统时间字段由后端设置，不使用前端传递的值
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    User savedUser = userService.save(user);
    return ResponseEntity.ok(savedUser);
}
```

---

## 8. API 设计规范

### 8.1 HTTP 方法

| 方法 | 用途 |
|------|------|
| GET | 获取资源 |
| POST | 创建资源 |
| PUT | 完全更新资源 |
| PATCH | 部分更新资源 |
| DELETE | 删除资源 |

### 8.2 状态码

| 状态码 | 含义 |
|--------|------|
| 200 OK | 请求成功 |
| 201 Created | 资源创建成功 |
| 400 Bad Request | 客户端错误 |
| 401 Unauthorized | 未授权 |
| 404 Not Found | 资源不存在 |
| 500 Internal Server Error | 服务器错误 |

### 8.3 请求/响应格式

- 使用 JSON 作为主要数据交换格式
- 统一错误响应格式
- 分页数据应包含总数、页码等信息

**通用错误响应格式**:
```json
{
  "error": "错误信息",
  "timestamp": 1700000000000,
  "path": "/api/endpoint"
}
```

---

## 9. 数据库设计规范

### 9.1 命名规范

- 表名使用复数形式，小写字母加下划线分隔
- 字段名使用小写字母加下划线分隔
- 主键统一命名为 `id`
- 外键命名格式为：`表名_id`

### 9.2 数据类型

- 时间字段存储为 BIGINT 类型以支持 13 位时间戳
- 字符串字段根据实际需要选择 VARCHAR 长度
- 数值字段根据范围选择合适的数据类型

---

## 10. 运行与构建

### 10.1 开发运行

**后端运行**:
```bash
mvn -f backend/pom.xml spring-boot:run
```

**前端运行**:
```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

### 10.2 构建

**后端构建**:
```bash
mvn -f backend/pom.xml -DskipTests package
```
输出位置：`backend/target`

**前端构建**:
```bash
npm --prefix frontend run build
```

### 10.3 构建约束

- 后端输出在 `backend/target`，避免在仓库根目录执行 Maven 构建
- 前后端分离，目录结构独立，降低耦合并便于扩展

---

## 11. GraalVM Native Image

### 11.1 目标版本

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.2.2 |
| JDK | 21 |
| GraalVM | 23.1.2 |
| GraalVM Build Tools | 0.10.2 |

### 11.2 构建命令

**仅生成 AOT 产物**（可用于排查 AOT 报错）:
```bash
mvn -f backend/pom.xml -Paot -DskipTests package
```

**构建 native 可执行文件**:
```bash
mvn -f backend/pom.xml -Pnative -DskipTests package
```

**输出路径**:
- Linux/macOS: `backend/target/native/ass-kicker`
- Windows: `backend/target/native/ass-kicker.exe`

**运行示例**:
```bash
backend/target/native/ass-kicker --spring.profiles.active=native
```

### 11.3 运行约束

- 禁止反射、JDK 动态代理、CGLIB 等运行时代理机制
- 所有 `@Configuration` 关闭 `proxyBeanMethods`，避免 CGLIB 代理
- native profile 下关闭 springdoc（OpenAPI），避免依赖运行时反射扫描
- AOT 资源提示通过 `NativeRuntimeHints` 显式注册（配置文件、Flyway SQL）

### 11.4 依赖扫描结果

- 代码扫描未发现 `java.lang.reflect`、`Proxy`、`cglib` 等直接使用点
- Spring Data Repository 将在 AOT 阶段生成实现，避免运行时代理生成
- `springdoc-openapi` 在 native profile 中禁用，防止扫描/反射路径引入

**相关依赖注意事项**:
- `spring-boot-starter-security`: 使用过滤器链配置，避免方法级 AOP 代理
- `spring-boot-starter-data-r2dbc`: Repository 在 AOT 阶段生成实现类
- `springdoc-openapi-starter-webflux-ui`: native profile 已禁用
- `jjwt-jackson`: 依赖 Jackson，新增序列化类型需确保 AOT 可见

### 11.5 回滚与兜底

默认保留 JVM 构建方式，作为 native 构建失败或部署问题的兜底：

```bash
mvn -f backend/pom.xml -DskipTests package
```

### 11.6 CI 集成

CI 中新增 native 构建与启动 smoke test，使用 `/health` 路由验证进程启动成功。

---

## 12. 关键架构决策

| 决策 | 说明 |
|------|------|
| WebFlux RouterFunction | 便于扩展多路由风格并保持清晰分层 |
| 前后端分离 | 目录结构独立，降低耦合并便于扩展 |
| Vite 构建 | 提升前端开发体验与构建效率 |
| MapStruct 映射 | 编译时类型安全，性能优于运行时反射 |
| 异步日志 | 高吞吐场景下减少日志对主流程的影响 |
| 请求追踪 ID | 便于分布式系统的问题定位和日志关联 |
| GraalVM Native | 提供快速启动和低内存占用的部署选项 |

---

## 附录 A: 模板 API 参考

### A.1 模板对象结构

```json
{
  "id": 1,
  "name": "模板名称",
  "description": "模板描述",
  "content": "模板内容",
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### A.2 API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/templates | 创建模板 |
| GET | /api/templates/{id} | 获取单个模板 |
| PUT | /api/templates/{id} | 更新模板 |
| DELETE | /api/templates/{id} | 删除模板 |

### A.3 请求示例

**创建模板**:
```bash
curl -X POST \
  http://your-domain.com/api/templates \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token-here' \
  -d '{
    "name": "API 测试模板",
    "description": "通过 API 创建的模板",
    "content": "这是一个测试模板的内容"
  }'
```

---

## 附录 B: 测试策略

### B.1 Handler 测试

使用 WebTestClient 对 Handler 进行测试：

```java
@Test
void shouldCreateTemplateSuccessfully() {
    Template inputTemplate = new Template("Test Template", "Test Content");
    Template savedTemplate = new Template("Test Template", "Test Content");
    savedTemplate.setId(1L);

    when(templateService.createTemplate(any(Template.class))).thenReturn(Mono.just(savedTemplate));

    webTestClient.post()
            .uri("/api/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(inputTemplate)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Template.class)
            .isEqualTo(savedTemplate);
}
```

### B.2 性能测试

**MapStruct 性能测试**:
```bash
cd backend
mvn -DperfTest=true -Dtest=UserMapperPerformanceTest test
```

**日志性能测试**:
```bash
mvn -DperfTests=true -Dtest=LoggingPerformanceTest test
```

---

## 附录 C: 迁移指南

从 Controller 模式迁移到 Router/Handler 模式的主要步骤：

1. 创建 Handler 类，将 Controller 中的业务逻辑迁移到 Handler 方法
2. 创建 Router 类，定义路由映射
3. 更新 Service 层以支持响应式编程
4. 更新 Repository 层以支持响应式操作（如适用）
5. 调整测试代码以适应新的模式

---

*文档版本：1.0*
*最后更新：2026 年 2 月*
