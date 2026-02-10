## Why

现有后端依赖反射与运行时代理，导致原生镜像编译受阻、启动时间和资源占用偏高；引入 Spring AOT 与 GraalVM 可显著降低冷启动成本，满足容器化与弹性扩缩的需求。

## What Changes

- 引入 Spring AOT 构建链路，产出 AOT 生成物并使用 GraalVM 编译为可执行文件
- 约束代码与依赖，禁止使用反射、JDK 动态代理、CGLIB 等运行时代理机制及其衍生特性
- 增加 native 可执行文件的构建与运行方式（本地与 CI）
- 明确不可用特性与替代方案的约束清单，保证可编译、可运行

## Capabilities

### New Capabilities
- `spring-aot-native-image`: 后端可通过 Spring AOT + GraalVM 构建为 native 可执行文件，并在不使用反射/代理的约束下运行

### Modified Capabilities

## Impact

- 构建系统与 CI：引入 GraalVM 工具链与 AOT 构建流程
- 代码实现：Spring 配置、Bean 注册、序列化/映射库使用方式可能需调整
- 运行与部署：可执行文件发布、启动脚本、资源占用与镜像大小