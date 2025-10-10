# Multi-stage build: use Gradle to build the application
FROM gradle:8.10-jdk25 as builder
WORKDIR /app

# Copy only the necessary files to build the orders-service
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle.properties /app/
COPY gradle /app/gradle
COPY common-events/src /app/common-events/src
COPY common-kafka/src /app/common-kafka/src
COPY common-persistence/src /app/common-persistence/src
COPY common-sagas/src /app/common-sagas/src
COPY services/orders-service/src /app/services/orders-service/src
COPY services/orders-service/build.gradle.kts /app/services/orders-service/build.gradle.kts

RUN gradle :services:orders-service:bootJar --no-daemon

# Runtime stage: use OpenJDK 25 for running the application
FROM openjdk:25-jdk-slim
LABEL org.opencontainers.image.source="https://github.com/${{ GITHUB_REPOSITORY }}"
LABEL org.opencontainers.image.description="Orders service for Kafka Transactional Microservices"
LABEL org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/services/orders-service/build/libs/orders-service-*.jar app.jar

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser /app
USER appuser

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]