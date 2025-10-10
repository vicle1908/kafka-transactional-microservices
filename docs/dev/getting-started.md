# Development Workflow

This document describes the standard development workflow for the Kafka Transactional Microservices project.

## Prerequisites

- JDK 25 (Temurin recommended); ensure `java -version` reports 25.x
- Docker Desktop for running local dependencies via `infra/compose.yml` (pure KRaft mode, no ZooKeeper dependency)
- IntelliJ IDEA (2025.2+) with Kotlin and Spring plugins enabled

## Initial Setup

1. Clone the repository and install the Git hooks if provided
2. Start the local infrastructure stack:
   ```bash
   docker compose -f infra/compose.yml up -d
   ```
3. Verify tooling:
   ```bash
   ./gradlew versionCheck
   ```
4. Import the Gradle project into IntelliJ; enable the Kotlin code style shipped with the repo (see `.editorconfig`)
   - When collaborating via JetBrains MCP server, use tools such as `open_file_in_editor` for navigation and `get_file_problems` to surface IntelliJ inspections without leaving the shared environment

## Environment configuration for local/dev

This repository uses 12‑Factor, env‑driven configuration for local development.

1) Create local env files (examples provided):
   ```bash
   cp .env.example .env
   cp infra/.env.example infra/.env
   ```

2) Auto‑load env (choose one):
   - Recommended (direnv):
     ```bash
     # one-time
     direnv allow
     ```
   - Shell fallback (no direnv):
     ```bash
     # each new shell
     source scripts/export-env.sh
     ```

3) Start local infrastructure with env file:
   - From repo root:
     ```bash
     docker compose --env-file infra/.env -f infra/compose.yml up -d
     ```
   - Or from infra directory (auto-loads infra/.env):
     ```bash
     cd infra && docker compose up -d
     ```

4) Run services (config comes from env, with safe defaults):
   ```bash
   ./gradlew :services:orders-service:bootRun
   # similarly for others
   ```

5) Optional: enable local tracing
   ```bash
   # in .env
   OTEL_ENABLED=true
   OTEL_ENDPOINT=localhost:4317
   ```

Security & Secrets
- Do not commit .env files (already git-ignored). Use *.env.example to share non-secret defaults.
- Staging/Prod secrets are sourced from Vault (see ADR‑0004 and docs/runbooks/vault.md). Local .env is for dev only.

## Building & Testing

### Run all checks (includes ktlint, detekt, Jacoco):
```bash
./gradlew clean check
```

### Execute module-specific tests:
```bash
./gradlew :common-persistence:test
```

### Lint only:
```bash
./gradlew ktlintCheck detekt
```

### Generate Avro classes:
```bash
./gradlew :common-events-avro:build
```

### Run integration tests with full environment:
```bash
./gradlew integrationTest
```

## Running the Microservices

To run the full saga orchestration, you need to start all the individual services in separate terminal sessions.

### 1. Start the Workflow Worker
```bash
./gradlew :temporal-pilot:bootRun
```

### 2. Start the Activity Workers
```bash
./gradlew :services:payments-service:bootRun
```
```bash
./gradlew :services:inventory-service:bootRun
```
```bash
./gradlew :services:notification-service:bootRun
```

### 3. Start the Order Service (Workflow Client)
```bash
./gradlew :services:orders-service:bootRun
```

Once all services are running, creating a new order via the `orders-service` API will trigger the distributed Temporal workflow.

## Coding Standards

- Kotlin is the default language for new modules and services; follow the conventions in `@AGENTS.md`
- Shared dependencies are managed via the version catalog `gradle/libs.versions.toml`
- Apply the layered architecture guidelines outlined in the service template docs (Phase 2 deliverables)
- Follow the Kafka transaction configuration guidelines in `docs/dev/kafka-transaction-config.md` to ensure exactly-once semantics

## Troubleshooting

- If Gradle cannot locate Kotlin or JDK, ensure `JAVA_HOME` points to JDK 25 (use `sdkman` or asdf if you switch frequently)
- For Docker issues, confirm ports 5432/8080/8081/8083/9092 are free before starting the compose stack

## Useful Commands

### Stop local infrastructure:
```bash
docker compose -f infra/compose.yml down
```

### Format Kotlin sources:
```bash
./gradlew ktlintFormat
```

### Generate coverage reports:
```bash
./gradlew jacocoTestReport
```

### Run schema compatibility checks:
```bash
./gradlew schemaCompatibilityCheck
```

### Export Avro schemas:
```bash
./gradlew exportAvroSchemas
```

## Module Development Workflow

### 1. Create a new service module:
```bash
# Create service directory structure
mkdir -p services/new-service/src/main/kotlin/com/example/newservice
mkdir -p services/new-service/src/test/kotlin/com/example/newservice
```

### 2. Add module to settings.gradle.kts:
```kotlin
include("services:new-service")
```

### 3. Create build.gradle.kts for the new service:
```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":common-events"))
    implementation(project(":common-kafka"))
    implementation(project(":common-persistence"))
    implementation(project(":common-sagas"))
    
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}
```

### 4. Implement the service following the hexagonal architecture:
- Domain layer: Business logic and entities
- Application layer: Use cases and transaction boundaries
- Adapter layer: REST controllers, Kafka listeners, repository implementations

### 5. Add database migrations:
```bash
# Create migration file in src/main/resources/db/migration/
# Follow Flyway naming convention: V<version>__<description>.sql
```

### 6. Implement transactional outbox pattern:
- Write business data and outbox event in the same transaction
- Configure Kafka producer with transactional support
- Use KafkaTransactionManager for exactly-once semantics

## Testing Strategy

### Unit Tests
- Test individual components in isolation
- Use mocks for external dependencies
- Located in `src/test/kotlin` directories

### Integration Tests
- Test components working together
- Use Testcontainers for real dependencies
- Located in `src/integration-test/kotlin` directories

### Contract Tests
- Verify service contracts and APIs
- Use Spring Cloud Contract or similar tools
- Ensure backward compatibility

### Load Tests
- Simulate production-like load
- Measure performance and scalability
- Identify bottlenecks and optimize

## Continuous Integration

The project uses GitHub Actions for CI with the following workflows:

### 1. CI Workflow (`.github/workflows/ci.yml`)
- Runs on push to main branch and pull requests
- Builds and tests all modules
- Runs linting and static analysis
- Checks version compatibility

### 2. Integration Test Workflow (`.github/workflows/integration-test.yml`)
- Spins up complete Kafka/DB environment
- Runs end-to-end integration tests
- Validates exactly-once semantics
- Monitors outbox table depth

### 3. Schema Compatibility Workflow (`.github/workflows/schema-compatibility.yml`)
- Validates Avro schema changes
- Ensures backward compatibility
- Prevents breaking changes

## Deployment

### Local Development
```bash
# Start all services
docker compose -f infra/compose.yml up -d

# Start individual service
./gradlew :services:orders-service:bootRun
```

### Staging Environment
- Kubernetes deployment with Helm charts
- Istio service mesh for traffic management
- Prometheus and Grafana for monitoring

### Production Environment
- Multi-region deployment for high availability
- Blue-green deployment strategy
- Canary releases with feature flags

## Monitoring and Observability

### Metrics
- Kafka transaction commit rates
- Outbox table depth
- Consumer lag
- Service response times

### Tracing
- Distributed tracing with OpenTelemetry
- End-to-end request tracking
- Performance bottleneck identification

### Logging
- Structured logging with correlation IDs
- Centralized log aggregation
- Alerting on error patterns

## Security

### Code Security
- Static analysis with SpotBugs and ErrorProne
- Dependency vulnerability scanning
- Regular security audits

### Runtime Security
- TLS encryption for all communications
- Authentication and authorization
- Secrets management with Vault

### Compliance
- GDPR and data privacy compliance
- Regular security assessments
- Audit trails for all operations