# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Dual Jackson support: Jackson 3.x for Spring Boot 4.0 features, Jackson 2.x for Temporal SDK compatibility
- New test starter dependencies for Spring Boot 4.0 modularized test slices
- Comprehensive observability stack with updated versions

### Changed

#### Core Platform Upgrades
- **Spring Boot**: 3.5.6 → 4.0.0 (Major version upgrade)
- **Spring Framework**: 6.2.11 → 7.0.1
- **Spring Kafka**: 3.3.10 → 4.0.0
- **Kotlin**: 2.2.20 → 2.2.21
- **Gradle**: 9.1.0 → 9.2.1

#### Messaging & Data
- **Kafka Clients**: 4.1.0 → 4.1.1
- **Avro**: 1.12.0 → 1.12.1
- **Flyway**: 11.14.0 → 11.16.0

#### gRPC & Observability
- **gRPC**: 1.76.0 → 1.77.0
- **OpenTelemetry**: 1.54.1 → 1.56.0
- **Protobuf**: 3.25.5 → 4.30.2

#### Docker Infrastructure
- **Kafka Image**: apache/kafka:4.1.0 → 4.1.1
- **Schema Registry**: confluentinc/cp-schema-registry:7.7.0 → 7.9.5
- **PostgreSQL**: postgres:18-alpine3.22 → 18.1-alpine
- **Redis**: redis:8-alpine3.22 → 8.4.0-alpine
- **Prometheus**: prom/prometheus:v3.6.0 → v3.7.3
- **Grafana**: grafana/grafana:main-ubuntu → 12.3.0
- **Jaeger**: jaegertracing/all-in-one:1.74.0 → 1.75.0
- **OpenTelemetry Collector**: otel/opentelemetry-collector-contrib:0.137.0 → 0.140.0
- **AKHQ**: tchiotludo/akhq:0.25.0 → 0.26.0
- **Elastic Stack**: 8.19.5 → 9.2.1 (Major version upgrade)

### Migration Notes

#### Spring Boot 4.0 Package Changes
The following package migrations are required:

| Old Package | New Package |
|-------------|-------------|
| `org.springframework.boot.autoconfigure.flyway` | `org.springframework.boot.flyway.autoconfigure` |
| `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `org.springframework.boot.test.autoconfigure.jdbc` | `org.springframework.boot.jdbc.test.autoconfigure` |
| `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |

#### Test Annotations
- `@MockBean` → `@MockitoBean` (from `org.springframework.test.context.bean.override.mockito`)

#### Jackson Migration
- Production code uses Jackson 3.x (`tools.jackson.core.*`)
- Temporal SDK maintains Jackson 2.x (`com.fasterxml.jackson.*`) for compatibility
- JSR-310 support is built into Jackson 3.x (no separate module needed)

#### Test Dependencies
New test starter dependencies required for Spring Boot 4.0:
- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-webmvc-test`
- `spring-boot-starter-flyway` (replaces `flyway-core`)

### Deprecated
- Jackson 2.x usage in production code (migrate to Jackson 3.x)
- Old Spring Boot test package imports

#### Temporal SDK
- **Temporal SDK**: Migrated from `temporal-sdk` with manual gRPC exclusions to `temporal-shaded`
- `temporal-shaded` relocates gRPC/Netty/Protobuf to `io.temporal.shaded.*` packages
- Eliminates version conflicts between Temporal's gRPC 1.58.1 and project's gRPC 1.77.0
- Cleaner dependency management without fragile exclusion rules

### Security
- Updated all dependencies to latest stable versions with security patches
- Jackson 3.x includes security improvements over 2.x
