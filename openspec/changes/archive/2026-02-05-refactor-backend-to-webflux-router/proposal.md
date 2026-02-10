## Why

当前后端使用传统的Spring MVC Controller方式实现HTTP接口，为了提高系统的响应性和吞吐量，需要重构为Spring WebFlux的Router函数式编程模型。同时移除不再需要的e2e测试文件夹。

## What Changes

- **BREAKING**: 将后端HTTP接口从Controller模式重构为WebFlux Router/Handler模式
- **BREAKING**: 移除e2e-tests文件夹及其中的所有内容
- 修改所有现有的REST API端点实现方式
- 更新相关依赖配置以支持WebFlux

## Capabilities

### Modified Capabilities
- `java21-spring-webflux-backend`: 更新后端架构以使用WebFlux Router/Handler模式

## Impact

- 所有后端API实现将被重构
- 需要更新前端API调用适配新的路由
- 移除e2e-tests文件夹
- 项目依赖需要调整以适应WebFlux
- 现有的Controller类将被替换为Handler和Router类