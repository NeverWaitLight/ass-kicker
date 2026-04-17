本后端项目基于 Spring Boot 3 + WebFlux，运行时依赖 MongoDB 与 Kafka。

## 技术栈

- Java 21
- Spring Boot 3.2
- Spring WebFlux
- Spring Security + JWT
- Reactive MongoDB
- Spring Kafka

## 本地启动

默认配置见 [src/main/resources/application.yml](src/main/resources/application.yml)。

启动前请准备：

- MongoDB：`localhost:27017`
- Kafka：`localhost:9092`

运行：

```bash
mvn spring-boot:run
```

接口文档：

```text
http://localhost:8080/scalar
```

健康检查：

```text
http://localhost:8080/actuator/health
```

## Docker

推荐直接使用仓库根目录下的 [../deploy/docker-compose.yml](../deploy/docker-compose.yml) 联合启动 MongoDB、Kafka、manager、worker。
