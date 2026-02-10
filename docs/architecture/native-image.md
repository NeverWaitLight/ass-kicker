# Spring AOT 与 GraalVM Native Image

## 目标版本

- Spring Boot：3.2.2（与 `backend/pom.xml` 一致）
- JDK：21
- GraalVM：23.1.2（native 构建）
- GraalVM Build Tools：0.10.2

## 本地构建与输出

仅生成 AOT 产物（可用于排查 AOT 报错）：

```bash
mvn -f backend/pom.xml -Paot -DskipTests package
```

构建 native 可执行文件：

```bash
mvn -f backend/pom.xml -Pnative -DskipTests package
```

输出路径约定：

- Linux/macOS：`backend/target/native/ass-kicker`
- Windows：`backend/target/native/ass-kicker.exe`

运行示例（native profile 会禁用 OpenAPI）：

```bash
backend/target/native/ass-kicker --spring.profiles.active=native
```

## 运行约束

- 禁止反射、JDK 动态代理、CGLIB 等运行时代理机制。
- 所有 `@Configuration` 关闭 `proxyBeanMethods`，避免 CGLIB 代理。
- native profile 下关闭 springdoc（OpenAPI），避免依赖运行时反射扫描。
- AOT 资源提示通过 `NativeRuntimeHints` 显式注册（配置文件、Flyway SQL）。

## 扫描结果

- 代码扫描未发现 `java.lang.reflect`、`Proxy`、`cglib` 等直接使用点。
- Spring Data Repository 将在 AOT 阶段生成实现，避免运行时代理生成。
- `springdoc-openapi` 在 native profile 中禁用，防止扫描/反射路径引入。

相关依赖（需持续关注）：

- `spring-boot-starter-security`：使用过滤器链配置，避免方法级 AOP 代理。
- `spring-boot-starter-data-r2dbc`：Repository 在 AOT 阶段生成实现类。
- `springdoc-openapi-starter-webflux-ui`：native profile 已禁用。
- `jjwt-jackson`：依赖 Jackson，新增序列化类型需确保 AOT 可见。

## 回滚与兜底

- 默认仍保留 JVM 构建方式，作为 native 构建失败或部署问题的兜底。
- JVM 兜底构建命令：

```bash
mvn -f backend/pom.xml -DskipTests package
```

## CI

- CI 中新增 native 构建与启动 smoke test（见 `.github/workflows/native-build.yml`）。
- smoke test 使用 `/health` 路由验证进程启动成功。
