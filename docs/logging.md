# 日志与请求追踪

## 异步日志配置

后端使用 Logback + `AsyncAppender` 实现异步日志，配置文件位于 `backend/src/main/resources/logback-spring.xml`。

关键参数：
- `logging.async.queue-size`：异步队列大小，默认 `1024`
- `logging.async.discarding-threshold`：丢弃阈值，默认 `0`（不丢弃）

日志输出使用 `STDOUT`，并通过 `ASYNC_STDOUT` 异步包装，适合容器化运行环境。

## 请求追踪 ID

系统为每个请求生成或复用 `X-Request-Id`：
- 如果请求头已携带 `X-Request-Id`，直接复用（便于跨服务传递）
- 如果缺失，则生成新的 UUID
- 响应头会回写相同的 `X-Request-Id`

请求 ID 会写入 MDC（key 为 `requestId`），并在日志格式中输出：
- 日志格式包含 `%X{requestId:-}`，确保所有日志可追踪到请求 ID

## 依赖说明

- `com.lmax:disruptor`：高性能队列组件，为异步日志的高吞吐场景提供支撑。
- MDC 支持来自 Spring Boot 默认日志栈（SLF4J + Logback），无需额外引入。

## 性能测试

提供了可选的性能测试用于比较同步与异步日志：
- 测试类：`backend/src/test/java/com/github/waitlight/asskicker/perf/LoggingPerformanceTest.java`
- 运行示例：

```bash
mvn -DperfTests=true -Dtest=LoggingPerformanceTest test
```
