# Version Matrix

This document lists the current, candidate, and fallback versions for all major components in the Kafka Transactional Microservices project.

## Platform Components

| Component | Current Stable | Candidate | Fallback | Notes |
|-----------|----------------|-----------|----------|-------|
| Java | 25 | 25.0.x patches | 23 LTS (21 if required) | Toolchain pinned via `org.gradle.java.installations.paths` to `/Library/Java/JavaVirtualMachines/zulu-25.jdk`; override `java.languageVersion` when a lower runtime is needed |
| Kotlin | 2.2.21 | 2.2.x patches | 2.2.20 | Latest stable with bug fixes |
| Spring Boot | 4.0.0 | 4.0.x patches | 3.5.6 | Major upgrade - requires Spring Framework 7.x and Jackson 3.x |
| Spring Framework | 7.0.1 | 7.0.x patches | 6.2.11 | Aligned with Spring Boot 4.0.x |
| Spring for Apache Kafka | 4.0.0 | 4.0.x patches | 3.3.10 | Enhanced EOS support with Spring Boot 4.0 |
| Apache Kafka (broker) | 4.1.1 | 4.1.x patches, 4.2 RC | 4.1.0 | Maintenance release with bug fixes |
| Debezium | 3.3.0.Final | 3.3.x patches | 3.2.2.Final | Ensure connector compatibility with Kafka 4.1 |
| Apache Avro | 1.12.1 | 1.12.x patches | 1.12.0 | Shared schemas via `common-events-avro`; keep registry compatibility checks in CI |
| PostgreSQL | 18.1 | 18.x patches | 18.0 | Test pg_upgrade paths before production rollout |
| Temporal SDK | 1.32.1 (shaded) | 1.33.x patches | 1.30.x | Uses `temporal-shaded` artifact which relocates gRPC/Netty/Protobuf to `io.temporal.shaded.*` packages for isolation |
| Istio | 1.24.x ambient | 1.25/1.26 | N/A | Track ambient multicluster features |
| Redis | 8.4.0 | Managed options | 8.2.2 | Standardize TLS/auth config |
| Vault | 1.15.x | 1.16 RC | N/A | Evaluate enterprise vs OSS; ensure auto-unseal |
| CDN | TBD | Provider evaluation | N/A | Complete Phase 3 assessment |
| Gradle | 9.2.1 | 9.2.x patches | 9.1.0 | Windows ARM support, performance improvements |
| gRPC | 1.77.0 | 1.77.x patches | 1.76.0 | Latest stable release |
| OpenTelemetry | 1.56.0 | 1.56.x patches | 1.54.1 | Declarative config support, profile exporters |
| Flyway | 11.16.0 | 11.x patches | 11.14.0 | Latest database support |

## Shared Module Dependencies

| Module | Library | Version | Purpose |
|--------|---------|---------|---------|
| common-kafka | spring-kafka | 4.0.0 | Transactional producer/consumer support |
| common-kafka | kafka-clients | 4.1.1 | Exactly-once semantics, idempotent producers |
| common-persistence | spring-data-jpa | 4.0.0 | JPA entity management |
| common-persistence | hibernate-core | 7.0.0 | ORM implementation |
| common-persistence | spring-boot-starter-flyway | 4.0.0 | Database migration management |
| common-sagas | spring-data-jpa | 4.0.0 | Saga state persistence |
| common-events-avro | avro | 1.12.1 | Avro schema serialization |
| common-proto | protobuf-java | 4.30.2 | Protocol buffer serialization |
| common-proto | grpc-stub | 1.77.0 | gRPC service stubs |
| common-temporal | temporal-shaded | 1.32.1 | Workflow orchestration (shaded artifact with relocated gRPC/Netty/Protobuf) |
| common-temporal | temporal-spring-boot-starter | 1.32.1 | Spring integration for Temporal |
| common-cache | spring-boot-starter-data-redis | 4.0.0 | Redis client integration |
| common-cache | spring-boot-starter-cache | 4.0.0 | Spring Cache abstraction |
| all services | jackson-core (3.x) | 3.0.3 | JSON serialization (new Spring Boot 4.0 features) |
| common-temporal | jackson-databind (2.x) | 2.20.0 | JSON serialization (Temporal SDK compatibility) |

## Service Dependencies

| Service | Module Dependencies | Notes |
|---------|---------------------|-------|
| orders-service | common-events, common-kafka, common-persistence, common-sagas, common-observability | Order lifecycle management |
| payments-service | common-events, common-kafka, common-persistence, common-sagas, common-observability | Payment processing and reconciliation |
| inventory-service | common-events, common-kafka, common-persistence, common-sagas, common-observability | Stock management and reservation |
| notification-service | common-events, common-kafka, common-persistence, common-observability | Notification delivery |

## Infrastructure Components

| Component | Version | Configuration File | Purpose |
|-----------|---------|-------------------|---------|
| Kafka Brokers | 4.1.1 | infra/compose.yml | Event streaming backbone |
| Schema Registry | 7.9.5 | infra/compose.yml | Avro schema management |
| PostgreSQL | 18.1-alpine | infra/compose.yml | Primary data store |
| Debezium Connect | 3.3.0.Final | infra/compose.yml | CDC and outbox event router |
| AKHQ | 0.26.0 | infra/compose.yml | Kafka management UI |
| Redis | 8.4.0-alpine | infra/compose.yml | Caching layer |
| Prometheus | v3.7.3 | infra/prometheus/prometheus.yml | Metrics collection |
| Grafana | 12.3.0 | infra/grafana/provisioning/ | Monitoring dashboards |
| Jaeger | 1.75.0 | infra/compose.yml | Distributed tracing |
| OpenTelemetry Collector | 0.140.0 | infra/compose.yml | Telemetry collection |
| Elasticsearch | 9.2.1 | infra/compose.yml | Log aggregation |
| Logstash | 9.2.1 | infra/compose.yml | Log processing |
| Kibana | 9.1.3 | infra/compose.yml | Log visualization |
| Filebeat | 9.2.1 | infra/compose.yml | Log shipping |

## Compatibility Notes

1. **Spring Boot and Spring for Apache Kafka**: These versions must be aligned to ensure proper transactional support and exactly-once semantics.

2. **Kafka Client and Broker Versions**: Client version should match or be compatible with broker version for optimal performance and feature support.

3. **Debezium and Kafka Compatibility**: Debezium connectors must be compatible with both Kafka broker version and database version.

4. **Avro and Schema Registry**: Avro version in services must be compatible with Schema Registry version for schema evolution.

5. **Temporal SDK and Server**: SDK version should be compatible with Temporal server version for workflow orchestration.

## Update Policies

1. **Patch Updates**: Apply promptly for bug fixes and security patches.
2. **Minor Updates**: Test in staging environment before promoting to production.
3. **Major Updates**: Thorough evaluation, testing, and migration planning required.
4. **Fallback Activation**: Use fallback versions when current versions have critical issues.

## CI/CD Validation

All version combinations are validated through:

- Gradle `versionCheck` task
- Schema compatibility checks
- Integration tests with full environment
- Security scanning
- Performance benchmarks

Any incompatible version combination will cause CI/CD pipeline failures.
