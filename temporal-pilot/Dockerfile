# Multi-stage build: use Gradle to build the application
FROM gradle:8.10-jdk25 as builder
WORKDIR /app

# Copy only the necessary files to build the temporal-pilot
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle.properties /app/
COPY gradle /app/gradle
COPY common-temporal/src /app/common-temporal/src
COPY temporal-pilot/src /app/temporal-pilot/src
COPY temporal-pilot/build.gradle.kts /app/temporal-pilot/build.gradle.kts

RUN gradle :temporal-pilot:bootJar --no-daemon

# Runtime stage: use OpenJDK 25 for running the application
FROM openjdk:25-jdk-slim
LABEL org.opencontainers.image.source="https://github.com/${{ GITHUB_REPOSITORY }}"
LABEL org.opencontainers.image.description="Temporal Pilot service for Kafka Transactional Microservices"
LABEL org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/temporal-pilot/build/libs/temporal-pilot-*.jar app.jar

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser /app
USER appuser

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]