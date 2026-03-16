## 压测结果

### 基线

| 并发 | 实际TPS | P50(ms) | P95(ms) | P99(ms) |
| ---- | ------- | ------- | ------- | ------- |
| 1000 | 2669.3  | 327.8   | 530.9   | 2717.1  |
| 2000 | 2539.7  | 689.1   | 1096.9  | 4122.3  |
| 3000 | 2656.3  | 1012.5  | 2554.7  | 4600.6  |
| 4000 | 2497.1  | 1471.7  | 2878.0  | 6767.2  |

**瓶颈**：每请求多条日志 MongoDB 每请求 3 次操作 日志 I/O 高并发下成为瓶颈

### 优化 #1：减少日志+减少数据库操作

**代码改进**：

- 发送日志 MongoDB 3 次写入改为 1 次
- 文件 `SendTaskConsumer.java`
- 删除 `logExecutionAsync`、`logExecution` 及中间过程 info 日志（SEND_TASK_RECEIVED、SEND_RESULT、SEND_RECORD_SAVED、SEND_TASK_FINISHED）
- 新增 `SEND_TASK_COMPLETED` 汇总日志 含 taskId、total、success、failed、status、durationMs
- 保留 warn 级别错误日志

| 并发 | 实际TPS | P50(ms) | P95(ms) | P99(ms) |
| ---- | ------- | ------- | ------- | ------- |
| 1000 | 2654.8  | 323.3   | 534.5   | 2705.9  |
| 2000 | 2380.6  | 673.5   | 1095.3  | 3933.6  |
| 3000 | 2421.4  | 1001.2  | 2380.1  | 4978.9  |
| 4000 | 2304.1  | 1375.7  | 2628.7  | 6283.0  |

**关键指标**：峰值 TPS 3085（并发 1800） 成功率 100% 第 10 轮 TPS 2990.5 < 饱和阈值 3060 触发停止

### 结论

- 固定四个并发档位下 新一轮数据里优化方案在 TPS 上全面低于基线 最大降幅约 8.9 说明在当前环境和样本下 减日志加合并写并没有带来吞吐提升 甚至存在一定退化 需要结合阶梯压测和更多轮次验证波动
- 延迟侧 P99 在 1000 2000 4000 三个档略优于基线 降幅在 0.4 到 7.2 之间 但在 3000 并发档 P99 反而升高约 8.2 整体呈现为吞吐略降 换来部分并发档位尾延迟的改善
- 结合两次压测结论 当前可以认为该优化更偏向削峰与平滑高并发长尾 而非简单提升极限 TPS 是否保留该改动 需要结合业务对 TPS 与尾延迟的权重选择 并建议后续在更长时间窗口和更细并发阶梯下复测确认趋势

## TODO

- 请求同步等待整任务完成：`SendHandler.buildAndSend` 使用 `Mono.fromRunnable(...).thenReturn(taskId)`，HTTP 响应时延和收件人数、下游通道耗时强绑定，高并发时容易把压力直接传到应用线程池。
- Reactive 链路里存在阻塞点：`SendTaskConsumer.processTask` 对 `templateManager.fill(...)` 与 `channelManager.selectChannel(...)` 使用 `.block()`，高并发下会放大线程切换与阻塞等待成本。
- 单任务内按收件人串行发送：`for (String recipient : recipients)` 串行调用 `sendChannel.send`，当单请求收件人较多时，任务耗时线性增长，吞吐受单任务长尾拖累。
- 审计写入放大：每个收件人都 `CompletableFuture.runAsync` 一次，并在异步线程里 `sendRecordRepository.save(...).block()`；高并发会形成大量小写请求，Mongo 写入与线程调度双重放大。
- 记录文档体积偏大：每条 `SendRecord` 重复保存 `params`、`recipients`、`renderedContent` 等字段，收件人越多写放大越明显，进一步挤占 Mongo 吞吐。
- 每任务重复创建发送通道：`createChannel` 每次解析 `propertiesJson` 并构造 channel 实例，热点模板下存在重复初始化开销。
- 模板渲染缺少编译缓存：`TemplateManager.render` 每次都 `mustacheFactory.compile`，在模板命中高的场景会产生不必要的重复编译成本。

**建议优先级**：

- 高优先级：审计改批量写入（`saveAll` 或缓冲批写），并移除异步线程中的 `.block()`。
- 中优先级：任务内收件人并行化（限流并发），避免单任务串行长尾。
- 中优先级：缓存可复用的 channel 实例与 Mustache 编译结果，减少重复构建与解析成本。
- 中优先级：精简 `SendRecord` 热路径字段，降单条写入体积。