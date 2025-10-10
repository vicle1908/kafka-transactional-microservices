# Build System Overview

## Multi-module Structure
- Root Gradle project manages shared modules:
  - `common-events` – shared event payloads and serializers (Kotlin).
  - `common-kafka` – Kafka producer/consumer configuration.
  - `common-persistence` – transactional outbox persistence components.
- `common-events-avro` hosts Avro schemas and generated classes shared across services.
- `common-observability` provides OpenTelemetry tracing, Micrometer metrics, and monitoring capabilities across services.
- Generated code lives under `common-events-avro/build/generated-main-avro-java`; downstream modules that need typed records should depend on `:common-events-avro`.
- Schema registration and compatibility checks will be wired into CI during Phase 3 once the registry connection is available.
- Gradle is configured with configuration cache, parallel execution, and VFS watching (`gradle.properties`); prefer invoking builds with `./gradlew --configuration-cache <task>` locally to match CI behavior.
- Version catalog (`gradle/libs.versions.toml`) centralizes dependency versions.
- Kotlin 2.2.20 is the primary language; Gradle Kotlin DSL used for build scripts.

## Gradle Wrapper
- The project expects the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
- Generate locally with an installed Gradle 9.1 (`gradle wrapper --gradle-version 9.1`) or copy from the reference project at `/Users/vinhlekhanh/Downloads/project/company/times/githubusers`.
- Commit the wrapper scripts so CI can execute `./gradlew` without additional tooling.

## Composite Builds (Future Services)
- Service applications live in `services/<name>` modules (e.g., `services/orders-service`) managed by the root build; they depend on shared modules (`common-events-avro`, `common-kafka`, `common-persistence`, `common-observability`).

## Build Logic
- Shared build conventions can live in `build-logic` (composite build) if we introduce custom plugins later.
- Keep root `build.gradle.kts` lightweight; push repeated configuration into convention plugins when the number of modules grows.

## Commands
- `./gradlew clean check` – run full verification (requires wrapper in repo)
- `./gradlew :common-persistence:test` – module-specific tests
- `./gradlew schemaCompatibilityCheck` – compile Avro schemas prior to registry publishing
- `./gradlew :services:orders-service:bootRun` – launch service locally once implementation is in place
- `./gradlew -q projects` – list modules

## IDE Integration
- Import the root project in IntelliJ IDEA; Gradle will automatically resolve composite builds and shared modules.
- Enable Kotlin code style and ensure the JDK 25 toolchain is configured.

## Publishing & Consumption
- Shared modules can be published to the internal artifact repository using `maven-publish` when needed.
- Composite builds allow services to depend on the latest local changes without publishing.

## References
- `settings.gradle.kts` for module and version catalog setup.
- `docs/dev/getting-started.md` for environment setup.
- Reference template repository: `/Users/vinhlekhanh/Downloads/project/company/times/githubusers`.
