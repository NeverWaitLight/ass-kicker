## Context

后端当前为 Spring（Maven 构建），以 JVM 方式运行，依赖反射与运行时代理能力；现阶段希望降低冷启动成本并满足容器化弹性扩缩需求，因此需要引入 Spring AOT 与 GraalVM native image，同时严格避免反射与代理机制。

## Goals / Non-Goals

**Goals:**
- 后端可稳定构建为 GraalVM native 可执行文件，并具备可重复的本地与 CI 构建流程
- 在运行时不使用反射、JDK 动态代理、CGLIB 等代理机制
- 提供明确的约束与替代方案，保证核心功能可运行且可测试

**Non-Goals:**
- 不对业务功能进行大规模重构或更换业务框架
- 不支持运行时插件化、动态类加载或脚本扩展
- 不以极致性能调优为主要目标（以可构建、可运行优先）

## Decisions

1. **采用 Spring AOT + GraalVM native image 作为目标形态**
   - 备选：迁移至 Quarkus/Micronaut 等框架
   - 理由：在保持现有 Spring 生态与开发模型的前提下，最小化迁移成本

2. **使用 Maven 原生构建链路（AOT 生成 + native 编译）而非仅依赖容器化 buildpacks**
   - 备选：使用 buildpacks 生成镜像后再提取可执行文件
   - 理由：本地与 CI 一致，便于控制 native-image 参数、失败诊断与缓存策略

3. **建立“无反射/无代理”强约束并在构建期失败**
   - 备选：允许反射并通过运行时配置（reflection-config）兜底
   - 理由：目标是完全避免运行时反射/代理，确保启动快、行为可预测

## Risks / Trade-offs

- [依赖库使用反射/代理导致无法编译] → 逐库梳理替换，优先选择 AOT 友好依赖；必要时拆分功能或降级
- [native 构建时间显著增加] → CI 缓存与分层构建；仅在关键分支/发布时启用
- [运行时行为差异（初始化时机、资源加载）] → 添加 native 专用测试与启动验证，明确限制清单

## Migration Plan

1. 增加 Maven 构建 profile 与 GraalVM 工具链配置，保持 JVM 构建不受影响
2. 扫描并移除反射/代理用法，替换为静态配置或显式注册
3. 引入 native 构建与 smoke 测试的 CI 任务
4. 部署灰度发布 native 可执行文件，保留 JVM 回滚方案

## Open Questions

- 目标 GraalVM 版本与 JDK 版本范围是否有统一约束？
- 现有依赖中哪些库需要替换或升级以满足 AOT 约束？
- 对外部署是否需要同时保留 JVM 产物作为长期兜底？