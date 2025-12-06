# Design Document: Dependency Version Update to Latest Stable Versions

## Overview

This design document outlines the approach for updating all project dependencies to their latest stable versions, including a major upgrade to Spring Boot 4.0.0. The update encompasses Gradle version catalog changes, Docker image updates in `infra/.env`, and comprehensive validation against official sources (GitHub releases, Maven Central, Docker Hub).

### Target Versions Summary

| Category | Component | Current | Target | Source |
|----------|-----------|---------|--------|--------|
| **Core Platform** | Spring Boot | 3.5.6 | 4.0.0 | GitHub/Maven Central |
| | Spring Framework | 6.2.11 | 7.0.1 | Maven Central |
| | Spring Kafka | 3.3.10 | 4.0.0 | Maven Central |
| | Kotlin | 2.2.20 | 2.2.21 | GitHub/Maven Central |
| **Build Tools** | Gradle | 9.1.0 | 9.2.1 | gradle.org |
| **Messaging** | Kafka Clients | 4.1.0 | 4.1.1 | Maven Central |
| | Avro | 1.12.0 | 1.12.0 | Maven Central (current) |
| **Database** | Flyway | 11.14.0 | 11.16.0 | Maven Central |
| | PostgreSQL Driver | 42.7.8 | 42.7.8 | Maven Central (current) |
| **gRPC** | gRPC | 1.76.0 | 1.77.0 | Maven Central |
| **Observability** | OpenTelemetry | 1.54.1 | 1.56.0 | Maven Central |
| **Testing** | Testcontainers | 1.21.3 | 1.21.3 | Maven Central (current) |
| **Code Quality** | Detekt | 1.23.8 | 1.23.9 | Maven Central |

## Architecture

### Update Strategy

The update follows a phased approach to minimize risk:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 1: Research & Validation               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   GitHub    │  │   Maven     │  │      Docker Hub         │ │
│  │  Releases   │  │  Central    │  │   / Quay.io / Elastic   │ │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘ │
│         │                │                      │               │
│         └────────────────┼──────────────────────┘               │
│                          ▼                                      │
│              ┌───────────────────────┐                          │
│              │  Version Validation   │                          │
│              │      Report           │                          │
│              └───────────────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 2: Configuration Updates               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ libs.versions   │  │   infra/.env    │  │ gradle-wrapper  │ │
│  │    .toml        │  │  Docker Images  │  │  .properties    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 3: Code Migration                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Spring Boot 4.0 Breaking Changes:                          ││
│  │  - Jackson 2.x → 3.x migration                              ││
│  │  - Deprecated API replacements                              ││
│  │  - Configuration property changes                           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 4: Validation & Testing                │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────────┐│
│  │  Build    │  │   Unit    │  │Integration│  │    Docker     ││
│  │  Check    │  │   Tests   │  │   Tests   │  │  Compose Up   ││
│  └───────────┘  └───────────┘  └───────────┘  └───────────────┘│
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼

┌─────────────────────────────────────────────────────────────────┐
│                    Phase 5: Documentation                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ version-matrix  │  │    AGENTS.md    │  │   CHANGELOG     │ │
│  │      .md        │  │   (baseline)    │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Version Catalog (`gradle/libs.versions.toml`)

Central configuration for all Gradle dependencies. Updates include:

```toml
[versions]
# Core Platform
kotlin = "2.2.21"
spring-boot = "4.0.0"
spring-kafka = "4.0.0"
jackson-core = "3.0.2"

# Build Tools
# (Gradle updated via wrapper)

# Messaging
kafka = "4.1.1"

# Database
flyway = "11.16.0"

# gRPC
grpc = "1.77.0"

# Observability
opentelemetry = "1.56.0"

# Code Quality
detekt = "1.23.9"
```

### 2. Infrastructure Environment (`infra/.env`)

Docker image versions validated against official sources:

```properties
# Validated against official sources
KAFKA_IMAGE=apache/kafka:4.1.1
DEBEZIUM_IMAGE=quay.io/debezium/connect:3.3
POSTGRES_IMAGE=postgres:18-alpine
REDIS_IMAGE=redis:8-alpine
# ... additional images
```

### 3. Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`)

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
```

## Data Models

### Version Update Tracking Model

```kotlin
data class VersionUpdate(
    val component: String,
    val currentVersion: String,
    val targetVersion: String,
    val source: ValidationSource,
    val status: UpdateStatus,
    val validatedAt: Instant
)

enum class ValidationSource {
    GITHUB_RELEASES,
    MAVEN_CENTRAL,
    DOCKER_HUB,
    QUAY_IO,
    GRADLE_ORG,
    ELASTIC_DOCKER
}

enum class UpdateStatus {
    PENDING,
    VALIDATED,
    APPLIED,
    TESTED,
    COMPLETED
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Build Compilation Success
*For any* Kotlin source file in the project, after version updates are applied, the file SHALL compile without errors when running `./gradlew compileKotlin`
**Validates: Requirements 1.5, 2.2**

### Property 2: Version Catalog Consistency
*For any* dependency declared in the version catalog, the version reference SHALL resolve to a valid artifact in Maven Central or the configured repository
**Validates: Requirements 1.1, 2.1, 4.1, 5.1, 6.1, 7.1**

### Property 3: Docker Image Availability
*For any* Docker image specified in `infra/.env`, the image tag SHALL exist and be pullable from its official registry (Docker Hub, Quay.io, or Elastic Docker)
**Validates: Requirements 11.1-11.12**

### Property 4: Infrastructure Version Alignment
*For any* component that appears in both the version catalog and Docker compose configuration, the versions SHALL be compatible (same major.minor for critical components)
**Validates: Requirements 12.1, 12.2**

### Property 5: Test Suite Execution
*For any* test class in the project, after version updates are applied, the test SHALL execute and pass when running `./gradlew test`
**Validates: Requirements 9.5, 13.2, 13.3**

### Property 6: Lint Check Compliance
*For any* Kotlin source file, after version updates are applied, the file SHALL pass ktlint and detekt checks without violations
**Validates: Requirements 10.3, 10.4, 13.4**

### Property 7: Documentation Version Accuracy
*For any* version listed in `docs/version-matrix.md`, the version SHALL match the corresponding entry in `gradle/libs.versions.toml` or `infra/.env`
**Validates: Requirements 12.3, 14.1**

## Error Handling

### Version Conflict Resolution

1. **Transitive Dependency Conflicts**: Use Gradle's `resolutionStrategy` to force specific versions
2. **Breaking API Changes**: Document migration steps and apply code changes
3. **Docker Image Pull Failures**: Fallback to previous stable version, document in version matrix

### Rollback Strategy

If critical issues are discovered:
1. Revert `gradle/libs.versions.toml` to previous commit
2. Revert `infra/.env` to previous commit
3. Revert `gradle/wrapper/gradle-wrapper.properties` to previous commit
4. Run `./gradlew clean build` to verify rollback

## Testing Strategy

### Dual Testing Approach

This update requires both unit testing and property-based testing:

#### Unit Tests
- Verify specific version values in configuration files
- Test individual component functionality after updates
- Validate Docker compose startup

#### Property-Based Tests
- Use **Kotest** property testing framework (already in project)
- Generate random test scenarios to verify system behavior
- Test version compatibility across components

### Test Categories

1. **Configuration Validation Tests**
   - Verify version catalog syntax
   - Verify Docker image tag format
   - Verify Gradle wrapper configuration

2. **Build Verification Tests**
   - Full project compilation
   - Dependency resolution
   - Plugin compatibility

3. **Integration Tests**
   - Kafka producer/consumer with new client version
   - Database migrations with new Flyway version
   - gRPC service communication with new gRPC version

4. **Infrastructure Tests**
   - Docker compose up/down cycle
   - Service health checks
   - Inter-service communication

### Property-Based Testing Configuration

```kotlin
// Example property test for version consistency
class VersionConsistencyPropertyTest : StringSpec({
    "all version catalog entries should resolve to valid artifacts" {
        forAll(versionCatalogEntryArb) { entry ->
            // Property: version should be resolvable
            canResolveArtifact(entry.group, entry.name, entry.version)
        }
    }
})
```

### Test Execution Order

1. `./gradlew clean` - Clean previous build artifacts
2. `./gradlew compileKotlin` - Verify compilation
3. `./gradlew test` - Run unit tests
4. `./gradlew integrationTest` - Run integration tests
5. `./gradlew ktlintCheck detekt` - Run code quality checks
6. `docker compose up -d` - Verify infrastructure startup
7. `docker compose down` - Clean up
