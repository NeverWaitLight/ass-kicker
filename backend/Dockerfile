FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
COPY distribution ./distribution
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/target/*.jar /app/app.jar

# Java 21：容器内按 cgroup 限制算堆，G1 + 常用稳态参数；运行时可覆盖 JAVA_OPTS
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=25.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
