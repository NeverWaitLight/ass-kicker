## 1. 构建链路

- [x] 1.1 确认后端 Spring Boot 与 JDK/GraalVM 目标版本，并更新 `backend/pom.xml` 的工具链/插件版本
- [x] 1.2 新增 Maven `native`/`aot` 构建 profile，接入 Spring AOT 与 GraalVM native 编译插件
- [x] 1.3 添加本地构建命令与输出路径约定文档

## 2. 代码与依赖约束

- [x] 2.1 扫描并列出反射、JDK 动态代理、CGLIB 使用点与相关依赖
- [x] 2.2 替换或移除上述用法，改为显式配置或静态注册实现
- [x] 2.3 补齐 AOT 所需显式配置（Bean 定义、资源加载等），确保无运行时反射

## 3. 验证与 CI

- [x] 3.1 配置 native-image 构建参数为“无反射/无代理即失败”
- [x] 3.2 添加 CI 任务：构建 native 可执行文件并执行启动 smoke test
- [x] 3.3 补充运行约束与回滚说明，确保 JVM 构建可作为兜底