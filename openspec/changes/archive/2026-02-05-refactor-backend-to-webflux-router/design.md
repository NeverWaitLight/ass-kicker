## Context

当前后端使用传统的Spring MVC Controller模式实现HTTP接口，这种模式基于阻塞式I/O和线程模型，对于高并发场景可能存在性能瓶颈。为了提升系统的响应性和吞吐量，需要迁移到Spring WebFlux的非阻塞编程模型。

## Goals / Non-Goals

**Goals:**
- 将后端HTTP接口从Controller模式重构为WebFlux Router/Handler模式
- 提升系统的响应性和处理高并发请求的能力
- 移除e2e-tests文件夹及其中的所有内容
- 保持API契约不变，确保前端兼容性
- 实现函数式、非阻塞的API处理

**Non-Goals:**
- 重构前端代码
- 更改业务逻辑实现
- 修改数据库模型或查询逻辑

## Decisions

1. **采用Router/Handler模式而非注解式Controller**：
   - 优势：更好的函数式编程支持，更细粒度的路由控制，非阻塞处理
   - 替代方案：保持Controller模式但使用WebFlux注解（如@Async）
   - 选择理由：Router/Handler模式更适合构建响应式系统，提供更好的性能和可扩展性

2. **保留现有API端点路径**：
   - 优势：前端无需修改API调用
   - 选择理由：减少迁移成本，避免破坏性变更

3. **逐步迁移策略**：
   - 优势：降低风险，便于测试
   - 选择理由：可以逐个模块进行重构，确保系统稳定性

## Risks / Trade-offs

[Risk] 学习曲线陡峭：WebFlux的响应式编程模型对团队来说可能较新
→ Mitigation: 提供培训和文档，从小模块开始实践

[Risk] 调试复杂性增加：响应式流的调试比传统阻塞式代码更困难
→ Mitigation: 引入适当的日志记录和监控工具

[Risk] 与某些库的兼容性问题：一些库可能不完全支持非阻塞操作
→ Mitigation: 仔细评估依赖项，必要时寻找替代方案

[Risk] 性能收益在低负载情况下不明显
→ Mitigation: 监控系统性能，确保改进确实有效

## Migration Plan

1. 添加WebFlux依赖到项目
2. 创建Handler类来处理业务逻辑
3. 创建Router类来定义路由映射
4. 逐步迁移现有Controller到新的Router/Handler模式
5. 移除Controller类
6. 移除e2e-tests文件夹
7. 测试API功能确保兼容性
8. 部署到预发布环境验证

## Open Questions

- 是否需要同时重构数据访问层以充分利用WebFlux的优势？
- 如何处理现有代码中可能存在的阻塞操作？