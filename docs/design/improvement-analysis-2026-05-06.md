# 📊 Ass Kicker 项目改进分析总报告

> 生成时间：2026-05-06

## 项目概况

| 项       | 详情                                                             |
| -------- | ---------------------------------------------------------------- |
| **定位** | 多渠道消息推送平台（SMS/Email/IM/Push）                          |
| **架构** | Manager（管理面）+ Worker（数据面）+ Kafka 解耦，CQRS 式设计     |
| **后端** | Java 21 / Spring Boot 3.2.2 / WebFlux / MongoDB Reactive / Kafka |
| **前端** | Vue 3 + Vite + Ant Design Vue                                    |
| **部署** | Docker Compose + 发行包                                          |
| **压测** | TPS 从基线 2619 提升到 5184（+97.9%）                            |

---

## 🔴 P0 — 必须立即修复

### 1. 安全漏洞

| 问题                                  | 位置                                | 说明                                                                                                          |
| ------------------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| **JWT Secret 硬编码默认值**           | `application.yml`                   | 默认值 `change-this-secret-to-32-chars-minimum`，部署忘改则任何人可伪造 JWT。应启动时校验长度并拒绝使用默认值 |
| **MongoDB 默认弱密码**                | `application.yml`                   | 默认 `admin:123456`，不应出现在代码仓库                                                                       |
| **`benchmark.py` 硬编码凭证**         | benchmark 目录                      | API Key 和 MongoDB URI 暴露在源码中，应从环境变量读取                                                         |
| **注册/登录接口无速率限制**           | AuthController                      | 可被暴力破解或批量注册垃圾用户                                                                                |
| **API Key 无格式校验**                | ApiKeyServerAuthenticationConverter | `Bearer abc` 等短 token 会触发 BCrypt 计算，是 CPU 消耗型攻击向量                                             |
| **`UserService.update` 密码覆盖漏洞** | UserService.update()                | `UpdateUserDTO` password=ignore 导致 null 覆盖已加密密码                                                      |
| **容器以 root 运行**                  | 所有 Dockerfile                     | 应添加非 root 用户                                                                                            |
| **`deploy/.env` 未在 gitignore 排除** | .gitignore                          | 真实密码文件可能被误提交                                                                                      |

### 2. CI/CD 缺陷

| 问题                             | 说明                                              |
| -------------------------------- | ------------------------------------------------- |
| **无 PR 验证管道**               | 只有 release 管道（tag push），没有 PR/push 验证  |
| **Release 管道 `-DskipTests`**   | 发布时完全跳过测试！                              |
| **无安全扫描**                   | 无依赖漏洞扫描、无 SAST                           |
| **UI Dockerfile nginx 配置缺失** | `ui/nginx/default.conf` 不存在，Docker 构建会失败 |

### 3. Kafka 核心瓶颈

| 问题                             | 说明                                                                         |
| -------------------------------- | ---------------------------------------------------------------------------- |
| **Consumer concurrency=1**       | Topic 3 分区但只有 1 消费者，2 个分区闲置。设为 3 可 **3x-10x TPS**          |
| **Consumer `.block()` 串行消费** | `SendTaskConsumer` 中 `sender.send(task).block()` 阻塞消费线程，整条链路串行 |

---

## 🟡 P1 — 近期改进

### 4. 代码质量（服务端）

| 问题                                                                                  | 位置              |
| ------------------------------------------------------------------------------------- | ----------------- |
| `AuthController.register` 首个用户 ADMIN 逻辑在 Controller 层 + 全表 count + 竞态条件 | AuthController    |
| `ChannelController.test` 40 行业务逻辑在 Controller                                   | ChannelController |
| `RecordService` 手动构造方法 + `@Value` 字段注入，违反项目规范                        | RecordService     |
| `RecordService.flushAsync` fire-and-forget，失败时记录丢失                            | RecordService     |
| 分页逻辑 5 个 Controller 完全重复                                                     | 所有 Controller   |
| `PageReq` 缺 `getOffset()` 方法，计算散布各处                                         | PageReq           |
| DTO 风格不一致（record vs @Data class）                                               | 各 DTO            |
| `NotFoundException` 构造方式不一致                                                    | 各 Service        |
| `ChannelController.delete` 缺 `@NotBlank` 校验                                        | ChannelController |
| `RecordController.page` 未使用 `PageReq`                                              | RecordController  |
| `Disruptor` 依赖未实际使用                                                            | pom.xml           |

### 5. 代码质量（前端）

| 问题                                     | 说明                                                   |
| ---------------------------------------- | ------------------------------------------------------ |
| **零测试覆盖**                           | Vitest/Playwright 配置存在但无测试文件                 |
| **`TemplateManagement.vue` 死代码**      | Options API + 原生 HTML，已被替代但仍存在              |
| **硬编码中文（80% 页面）**               | 仅 `GlobalVariableManagementPage` 和 `App.vue` 走 i18n |
| **单 JS Bundle 1691KB**                  | Ant Design 全量引入 + 路由无懒加载                     |
| **手动 reactive/ref store**              | 无 Pinia，无法用 Vue Devtools 追踪                     |
| **`ChannelConfigEditor` 350 行**         | 应拆分 composable                                      |
| **`kv.js` / `propertyRows.js` 功能重叠** | 统一为单一模块                                         |
| **Logo 477KB**                           | 应替换为 SVG 或优化 PNG                                |

### 6. 性能优化

| 问题                                                                  | 预估收益        |
| --------------------------------------------------------------------- | --------------- |
| `RecordRepository.saveAll()` 逐条 `concatMap+save` → 改为 `insertAll` | 20-30% 写入提升 |
| WebClient ConnectionProvider 无限制                                   | 防止资源耗尽    |
| MongoDB 连接池未显式配置                                              | 10-15% 查询提升 |
| 缺少 MongoDB 索引（task_id, key_prefix, recipient+channel_type）      | 查询延迟降低    |
| Kafka Producer 缺吞吐量参数（batch.size, linger.ms）                  | 5-10% 发送提升  |

### 7. Docker/部署

| 问题                                 | 说明                                |
| ------------------------------------ | ----------------------------------- |
| 所有服务缺 `deploy.resources.limits` | 容器可能无限占用资源                |
| Kafka CLUSTER_ID 硬编码              | 重建容器若 ID 不同会丢数据          |
| 缺少 Docker 网络隔离                 | 所有容器互通，应分 frontend/backend |
| 根目录缺 `.dockerignore`             | 构建可能包含 `.git`                 |
| 缺少 Prometheus/Grafana 配置         | 无监控能力                          |
| 缺少 systemd 服务模板                | 裸机部署缺服务管理                  |

---

## 🟢 P2 — 中期规划

| 领域         | 改进项                                                                                                                       |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| **可观测性** | Micrometer + Prometheus 指标导出、OpenTelemetry 分布式追踪、Caffeine `recordStats()`                                         |
| **安全加固** | API Key 过期机制、HTTPS/TLS、Kafka SASL 认证                                                                                 |
| **前端架构** | 迁移到 Pinia、Ant Design 按需引入、路由 lazy loading、ESLint/Prettier                                                        |
| **测试覆盖** | UserService/ChannelService/ApiKeyService/RecordService 单元测试、Controller WebFlux 端到端测试、前端 utils + composable 测试 |
| **CI/CD**    | Docker 镜像构建管道、安全扫描管道（Trivy）、E2E 测试管道                                                                     |
| **Kafka**    | Producer 吞吐量参数调优、Consumer concurrency=3                                                                              |

---

## 🟢 P3 — 长期演进

| 领域     | 改进项                                                            |
| -------- | ----------------------------------------------------------------- |
| **前端** | TypeScript 迁移、移动端侧栏 UX（hamburger + drawer）              |
| **后端** | Sender 从虚拟线程 block 改为纯响应式链路、Spring Boot 升级到 3.4+ |
| **依赖** | JJWT 0.12.x、MapStruct 1.6.x                                      |
| **部署** | Kubernetes Helm chart、蓝绿/金丝雀部署、日志聚合（Loki+Grafana）  |
| **规范** | 统一 DELETE 传参规范、Entity 注解规范（@Data vs @Getter/@Setter） |

---

## 📈 各维度评分总览

| 维度         | 评分    | 核心短板                                              |
| ------------ | ------- | ----------------------------------------------------- |
| **架构设计** | B+ (85) | CQRS 解耦清晰，但核心发送链路仍是阻塞模式             |
| **代码规范** | B (75)  | 分层好，但有违反（Controller 含业务逻辑、DTO 不统一） |
| **安全性**   | C (55)  | 硬编码凭证、无速率限制、容器 root 运行                |
| **测试覆盖** | D (30)  | 服务端 <20%，前端 0%                                  |
| **性能**     | B (75)  | 压测优化卓越，但 Kafka 单消费者是最大瓶颈             |
| **CI/CD**    | D (30)  | 只有 release 管道，跳测试，无安全扫描                 |
| **DevOps**   | B- (65) | 容器化基础扎实，缺资源限制和网络隔离                  |
| **前端**     | C+ (60) | 功能完整但零测试、国际化半成品、bundle 过大           |

**综合评分：B- (65/100)** — 项目核心功能完整、架构设计有亮点、压测优化出色，但 **安全性、测试覆盖、CI/CD** 是三大明显短板，需优先补齐。

---

## 附录：服务端代码深度审查

### 做得好的地方

1. **Reactor 管道惯用写法**：Service 层大量使用 `flatMap + switchIfEmpty`，避免 `hasElement + if/else` 反模式
2. **构造方法注入 + @RequiredArgsConstructor**：全项目严格遵守
3. **自定义异常层次体系**：`BusinessException → NotFoundException / BadRequestException / ConflictException / UnauthorizedException / PermissionDeniedException`，带 code + messageKey + args
4. **Caffeine 缓存 + 手动失效**：写操作后手动 `invalidate`，避免缓存脏数据
5. **DTO/VO/Entity 三层分离 + MapStruct 转换器**：职责清晰
6. **Channel 策略模式 + 工厂模式**：`Channel` 抽象基类 + `ChannelFactory` + `@ChannelImpl` 注解自动扫描
7. **Soft Delete 模式**：`UserEntity` 使用 `deleted_at` 字段
8. **Dockerfile 优化**：多阶段构建、JVM 参数容器化适配、健康检查

### 安全问题详解

#### 高危

1. **JWT Secret 硬编码默认值**：`change-this-secret-to-32-chars-minimum` 恰好 32 字节满足 HMAC-SHA256 最低要求，但众所周知。建议：启动时校验 secret 不等于默认值
2. **MongoDB 默认连接字符串含硬编码密码**：`mongodb://admin:123456@localhost:27017/asskicker`，代码仓库中不应出现真实凭据
3. **注册接口无速率限制**：`/v1/auth/register` 可被批量注册
4. **登录接口无速率限制**：`/v1/auth/login` 可被暴力破解
5. **API Key Bearer Token 误识别**：短 token 触发 BCrypt 计算，CPU 消耗型攻击

#### 中危

6. **CSRF 保护已禁用**：WebFlux + JSON API 场景下可接受
7. **API Key 无过期机制**：永久有效
8. **UserService.update password 未保护**：`UpdateUserDTO` password=ignore 导致 null 覆盖
9. **Manager SecurityConfig 无用规则**：`/v1/users/me` 和 `/v1/users/me/password` 路径无对应端点
10. **ChannelController.test 可滥用**：任何认证用户可向任意手机号/邮箱发送真实消息

### 测试覆盖评估

| 模块    | 测试类               | 覆盖领域          | 评估 |
| ------- | -------------------- | ----------------- | ---- |
| common  | `TemplateEngineTest` | Mustache 渲染     | ✅   |
| manager | `AuthServiceTest`    | 登录/刷新         | ✅   |
| worker  | `SendControllerTest` | Controller 层验证 | ✅   |
| worker  | `SenderTest`         | 发送路由          | ✅   |

**缺失的测试**：

- ❌ UserService、ChannelService、ApiKeyService、GlobalVariableService、TemplateService、RecordService
- ❌ 所有 Controller WebFlux 端到端测试
- ❌ JwtService、SecurityConfig 安全集成测试
- ❌ Repository 层查询测试

**估算覆盖率：< 20%**

---

## 附录：前端代码质量审查

### 做得好的地方

1. **Composition API 使用规范**：所有组件 `<script setup>`
2. **主题系统**：CSS 变量 + Ant Design `theme.algorithm` 双层适配
3. **国际化框架完整**：vue-i18n Composition mode + Ant Design locale 同步
4. **权限体系**：`CHANNEL_PERMISSIONS` + `hasPermission` + 路由守卫双重检查
5. **API 层**：`apiFetch` 自动 Token 刷新 + `unwrapData/unwrapPage` 统一响应处理
6. **KV 属性编辑器**：flat/hier 双模式 + 校验完备

### 关键问题详解

#### 零测试覆盖

Vitest + Playwright 配置存在但 **零测试文件**。`test/setup.js` 仅设置 `matchMedia` mock，未被消费。

#### TemplateManagement.vue 遗留代码

唯一 Options API 组件，使用原生 HTML/CSS（脱离 Ant Design 体系），`apiFetch` 导入路径错误，硬编码中文无 i18n。已被 `TemplateManagementPage.vue` 替代，应删除。

#### 硬编码中文（80% 页面）

仅 `GlobalVariableManagementPage.vue` 和 `App.vue` 完全走 i18n `t()`。其余所有页面组件硬编码中文。

#### Bundle 过大

- JS 1691KB（gzip 523KB），超过 500KB 警告阈值
- Ant Design Vue 全量引入 `app.use(Antd)`
- 路由无 lazy loading
- 未配置 `manualChunks`

#### Store 不规范

手动 `ref`/`reactive` 模块，无 Pinia Devtools 集成、`$subscribe`、`$reset`。`channels.js` 的 `channelPagination` 被多组件直接修改，违反单向数据流。

---

## 附录：DevOps 审计

### 容器化 ✅ 做得好

| 项目            | 详情                                              |
| --------------- | ------------------------------------------------- |
| 多阶段构建      | builder → runtime，最终镜像不含构建工具           |
| Alpine 基础镜像 | eclipse-temurin:21-jre-alpine / nginx:1.27-alpine |
| JVM 容器感知    | UseContainerSupport + MaxRAMPercentage=75.0       |
| OOM 自动退出    | ExitOnOutOfMemoryError                            |
| HEALTHCHECK     | 所有容器定义了健康检查                            |
| 依赖缓存层      | Manager Dockerfile 先 COPY pom.xml                |

### 容器化 ⚠️ 需改进

| 问题                         | 严重度 |
| ---------------------------- | ------ |
| UI Dockerfile nginx 配置缺失 | 🔴     |
| 所有容器以 root 运行         | 🔴     |
| 缺 `deploy.resources.limits` | 🔴     |
| 根目录缺 `.dockerignore`     | 🟡     |
| Kafka CLUSTER_ID 硬编码      | 🟡     |
| 缺 Docker 网络隔离           | 🟡     |
| 缺日志驱动配置               | 🟢     |

### CI/CD ✅ 做得好

| 项目         | 详情                                    |
| ------------ | --------------------------------------- |
| 版本解析灵活 | tag / dispatch / Maven version 三种方式 |
| 构建缓存     | Maven cache + npm cache                 |
| 双格式发行包 | .tar.gz + .zip                          |
| 条件发布     | tag push 才创建 GitHub Release          |

### CI/CD ⚠️ 需改进

| 问题                   | 严重度 |
| ---------------------- | ------ |
| `-DskipTests` 跳过测试 | 🔴     |
| 无 PR 验证管道         | 🔴     |
| 无安全扫描             | 🔴     |
| 缺 Docker 镜像构建管道 | 🟡     |
| 缺前端测试             | 🟡     |
| 单 job 设计            | 🟢     |

### 安全性 ⚠️ 需改进

| 问题                    | 严重度 |
| ----------------------- | ------ |
| JWT Secret 默认值       | 🔴     |
| MongoDB 弱密码          | 🔴     |
| benchmark.py 硬编码凭证 | 🔴     |
| 容器 root 运行          | 🔴     |
| 缺 HTTPS/TLS            | 🟡     |
| Kafka 无认证            | 🟡     |
| .env 未 gitignore       | 🟡     |

---

## 附录：性能工程审查

### 压测结果演进

| 阶段                       | 并发1000 TPS | P50(ms) | P99(ms) | 相比基线提升          |
| -------------------------- | ------------ | ------- | ------- | --------------------- |
| 基线                       | 2619.1       | 324.9   | 2816.1  | —                     |
| 优化#1（减少写入）         | 2654.8       | 323.3   | 2705.9  | +1.4%                 |
| 优化#2（去掉弹性调度器）   | 2635.9       | 342.5   | 1345.7  | +0.6%（P99 大幅改善） |
| 优化#3（缓冲池批量写入）   | 3213.1       | 276.6   | 1083.0  | **+22.7%**            |
| 优化#5（Channel实例缓存）  | 3133.3       | 280.9   | 1876.3  | +19.8%                |
| 优化#6（Mustache编译缓存） | 2985.2       | 285.0   | 2745.6  | +13.8%                |
| API Key认证                | 4577.1       | 191.8   | 1598.4  | **+75.1%**            |
| 去掉不必要日志             | 5184.7       | 167.2   | 372.9   | **+97.9%**            |

### 性能瓶颈排序

| 优先级 | 瓶颈                                      | 预估收益         |
| ------ | ----------------------------------------- | ---------------- |
| P0     | Kafka Consumer concurrency=1 + .block()   | **3x-10x TPS**   |
| P0     | RecordRepository.saveAll 逐条 → insertAll | **20-30%**       |
| P1     | Micrometer + Prometheus                   | 可观测性基础     |
| P1     | WebClient ConnectionProvider 配置         | 防止资源耗尽     |
| P1     | MongoDB 连接池调优                        | 10-15%           |
| P2     | MongoDB 索引补充                          | 查询延迟降低     |
| P2     | Kafka Producer 参数                       | 5-10%            |
| P2     | Sender 改为纯响应式                       | 消除线程调度开销 |
| P3     | Caffeine jitter 修正                      | 防雪崩可靠性     |

### 核心架构矛盾

项目选择了 Spring WebFlux（响应式）作为 HTTP 层，但核心发送链路是同步阻塞模式：

```
HTTP请求(Reactive) → Kafka(异步) → Consumer.block() → Sender(虚拟线程.block())
→ templateEngine.fill().block() → channel.send().block() → recordService.writeRecord(同步缓冲)
```

**推荐演进方向**：

- **方案A（渐进式）**：保持当前架构，修复关键瓶颈（concurrency=3、insertAll、连接池调优）
- **方案B（理想式）**：核心链路改为纯响应式（KafkaReceiver + 纯 Mono 链路）

---

## 附录：缺失的关键文件清单

| 文件                             | 状态      | 影响                       |
| -------------------------------- | --------- | -------------------------- |
| `ui/nginx/default.conf`          | ❌ 不存在 | UI Docker 构建失败         |
| 仓库根 `.dockerignore`           | ❌ 不存在 | Docker 构建可能包含 `.git` |
| `.github/workflows/ci.yml`       | ❌ 不存在 | 无 PR 验证                 |
| `.github/workflows/security.yml` | ❌ 不存在 | 无安全扫描                 |
| `deploy/.env` 在 gitignore       | ❌ 未排除 | 密码泄露风险               |
| Kubernetes manifests             | ❌ 不存在 | 无 K8s 部署                |
| Prometheus/Grafana 配置          | ❌ 不存在 | 无监控                     |
| systemd 服务模板                 | ❌ 不存在 | 裸机部署缺服务管理         |
