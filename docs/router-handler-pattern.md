# Router/Handler 模式文档

本文档介绍了项目中使用的Router/Handler设计模式，这是一种函数式、响应式的Web处理方式，用于替代传统的Controller模式。

## 概述

Router/Handler模式是Spring WebFlux提供的函数式编程模型，它将路由定义与请求处理分离：

- **Router**: 定义URL路径与处理函数之间的映射关系
- **Handler**: 实现具体的业务逻辑

## 设计原理

### 为什么使用Router/Handler模式？

1. **响应式编程**: 更好地支持异步、非阻塞的操作
2. **函数式编程**: 提供更简洁、可组合的API定义方式
3. **性能优化**: 在高并发场景下有更好的性能表现
4. **清晰分离**: 路由定义与业务逻辑分离，职责更加明确

### 与传统Controller模式的对比

| 特性 | Controller模式 | Router/Handler模式 |
|------|----------------|--------------------|
| 编程范式 | 注解驱动 | 函数式编程 |
| 并发模型 | 阻塞式 | 非阻塞式 |
| 性能 | 适用于一般场景 | 高并发场景更优 |
| 代码组织 | 类和方法 | 函数式组合 |

## 组件结构

### Handler组件

Handler负责实现具体的业务逻辑，通常包含以下特点：

```java
@Component
public class TemplateHandler implements BaseHandler {
    
    private final TemplateService templateService;

    public TemplateHandler(TemplateService templateService) {
        this.templateService = templateService;
    }

    public Mono<ServerResponse> createTemplate(ServerRequest request) {
        return request.bodyToMono(Template.class)
                .flatMap(template -> templateService.createTemplate(template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .onErrorResume(throwable -> {
                    // 错误处理逻辑
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to create template"));
                });
    }
    
    // 其他处理方法...
}
```

### Router组件

Router定义URL路径与Handler方法之间的映射关系：

```java
@Configuration
public class TemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> templateRoutes(TemplateHandler templateHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::createTemplate)
                .andRoute(RequestPredicates.GET("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::getTemplateById)
                .andRoute(RequestPredicates.PUT("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::updateTemplate)
                .andRoute(RequestPredicates.DELETE("/api/templates/{id}"), templateHandler::deleteTemplate);
    }
}
```

## 实现细节

### 响应式数据流

所有Handler方法都返回`Mono<ServerResponse>`或`Flux<ServerResponse>`，以支持响应式数据流：

```java
public Mono<ServerResponse> getTemplateById(ServerRequest request) {
    String id = request.pathVariable("id");
    return templateService.getTemplateById(id)
            .flatMap(template -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(template)))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(throwable -> {
                // 错误处理
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BodyInserters.fromValue("Failed to retrieve template"));
            });
}
```

### 错误处理

Router/Handler模式提供了统一的错误处理机制：

- `switchIfEmpty()`: 处理空结果情况
- `onErrorResume()`: 处理异常情况
- 自定义错误响应

## 最佳实践

### Handler设计原则

1. **单一职责**: 每个Handler专注于特定领域的业务逻辑
2. **无状态**: Handler应该是无状态的，依赖注入的服务来处理状态
3. **响应式**: 所有操作都应该返回Mono或Flux
4. **错误处理**: 为每个操作提供适当的错误处理

### Router设计原则

1. **清晰路由**: 路径定义应该清晰、直观
2. **一致性**: 路由命名和结构保持一致性
3. **安全性**: 结合安全配置确保路由安全

## 测试策略

### Handler测试

使用WebTestClient对Handler进行测试：

```java
@Test
void shouldCreateTemplateSuccessfully() {
    Template inputTemplate = new Template("Test Template", "Test Content");
    Template savedTemplate = new Template("Test Template", "Test Content");
    savedTemplate.setId(1L);
    
    when(templateService.createTemplate(any(Template.class))).thenReturn(Mono.just(savedTemplate));

    webTestClient.post()
            .uri("/api/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(inputTemplate)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Template.class)
            .isEqualTo(savedTemplate);
}
```

## 迁移指南

从Controller模式迁移到Router/Handler模式的主要步骤：

1. 创建Handler类，将Controller中的业务逻辑迁移到Handler方法
2. 创建Router类，定义路由映射
3. 更新Service层以支持响应式编程
4. 更新Repository层以支持响应式操作（如适用）
5. 调整测试代码以适应新的模式

## 性能考虑

Router/Handler模式在以下方面提供了性能改进：

- **资源利用**: 更少的线程和内存占用
- **并发处理**: 更好的高并发请求处理能力
- **响应时间**: 减少等待时间，提高响应速度

## 常见问题

### Q: 如何处理复杂的请求参数？
A: 使用`request.queryParam()`获取查询参数，`request.pathVariable()`获取路径变量，`request.bodyToMono()`获取请求体。

### Q: 如何实现认证和授权？
A: 通过Spring Security与WebFlux的集成来实现，可以在RouterFunction中添加安全过滤器。

### Q: 如何进行日志记录？
A: 可以使用`.doOnNext()`, `.doOnError()`, `.doFinally()`等操作符在响应式流中添加日志记录。