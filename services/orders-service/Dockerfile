FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /app
ENV GRADLE_OPTS="-Dorg.gradle.internal.http.socketTimeout=120000 -Dorg.gradle.internal.http.connectionTimeout=120000"
COPY gradle/ gradle/
COPY gradlew ./
COPY gradle.properties ./
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle/libs.versions.toml gradle/
COPY common-cache/build.gradle.kts common-cache/
COPY common-cache/src common-cache/src
COPY common-events/build.gradle.kts common-events/
COPY common-events/src common-events/src
COPY common-events-avro/build.gradle.kts common-events-avro/
COPY common-events-avro/src common-events-avro/src
COPY common-kafka/build.gradle.kts common-kafka/
COPY common-kafka/src common-kafka/src
COPY common-observability/build.gradle.kts common-observability/
COPY common-observability/src common-observability/src
COPY common-outbox-relay/build.gradle.kts common-outbox-relay/
COPY common-outbox-relay/src common-outbox-relay/src
COPY common-persistence/build.gradle.kts common-persistence/
COPY common-persistence/src common-persistence/src
COPY common-proto/build.gradle.kts common-proto/
COPY common-sagas/build.gradle.kts common-sagas/
COPY common-sagas/src common-sagas/src
COPY common-temporal/build.gradle.kts common-temporal/
COPY common-temporal/src common-temporal/src
COPY common-proto/src common-proto/src
RUN mkdir -p services/orders-service services/payments-service services/inventory-service services/notification-service
COPY services/orders-service/build.gradle.kts services/orders-service/
COPY services/payments-service/build.gradle.kts services/payments-service/
COPY services/inventory-service/build.gradle.kts services/inventory-service/
COPY services/notification-service/build.gradle.kts services/notification-service/
COPY services/orders-service/src services/orders-service/src
RUN ./gradlew :services:orders-service:bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Create non-root user for security (least-privilege principle)
RUN groupadd --gid 1000 appgroup && \
    useradd --uid 1000 --gid appgroup --shell /bin/false appuser && \
    chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /app/services/orders-service/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
