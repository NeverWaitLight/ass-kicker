## ADDED Requirements

### Requirement: 后端技术栈

定义后端技术栈基线：

- 使用 Java 21 作为运行时环境
- 使用 Spring WebFlux 作为 Web 框架
- 采用 Router/Handler 模式（非 Controller 注解）
- 实现 JWT 认证（access token 和 refresh token）
- 实现用户管理端点（基于角色的访问控制）

#### Scenario: 应用启动
- **WHEN** 启动 Spring Boot 应用
- **THEN** 应用成功启动并监听配置端口

#### Scenario: 健康检查
- **WHEN** 调用健康检查端点
- **THEN** 返回应用健康状态

---

### Requirement: 前端技术栈

定义前端技术栈基线：

- 使用 Vue 3 作为前端框架
- 使用 Ant Design Vue 作为 UI 组件库
- 应用成功启动并显示首页
- 首页包含标题和欢迎消息

#### Scenario: 应用启动
- **WHEN** 启动前端开发服务器或构建生产版本
- **THEN** 应用成功加载并显示首页

#### Scenario: 首页显示
- **WHEN** 用户访问首页
- **THEN** 显示应用标题和欢迎消息
