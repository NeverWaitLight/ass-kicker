FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY manager/pom.xml ./manager/pom.xml
RUN mvn -B -q -pl manager -am -DskipTests dependency:go-offline

COPY common/src ./common/src
COPY manager/src ./manager/src
RUN mvn -B -pl manager -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/manager/target/*.jar /app/app.jar

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
