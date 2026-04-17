FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

RUN apk add --no-cache nodejs npm

COPY svr/pom.xml ./svr/pom.xml
COPY svr/common/pom.xml ./svr/common/pom.xml
COPY svr/manager/pom.xml ./svr/manager/pom.xml
COPY ui/package*.json ./ui/
RUN mvn -B -q -f svr/pom.xml -pl manager -am -DskipTests dependency:go-offline

COPY svr/common/src ./svr/common/src
COPY svr/manager/src ./svr/manager/src
COPY ui ./ui
RUN mvn -B -f svr/pom.xml -pl manager -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/svr/manager/target/*.jar /app/app.jar

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
