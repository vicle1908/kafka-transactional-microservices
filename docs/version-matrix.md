# Version Matrix

This document lists the current, candidate, and fallback versions for all major components in the Kafka Transactional Microservices project.

## Platform Components

| Component | Current Stable | Candidate | Fallback | Notes |
|-----------|----------------|-----------|----------|-------|
| Java | 25 | 25.0.x patches | 23 LTS (21 if required) | Toolchain pinned via `org.gradle.java.installations.paths` to `/Library/Java/JavaVirtualMachines/zulu-25.jdk`; override `java.languageVersion` when a lower runtime is needed |
| Spring Boot | 3.5.6 | 3.5.x patches, 4.0 milestones | 3.4.x LTS | Monitor Spring Boot 4 milestone notes |
| Spring Framework | 6.2.11 | 6.2.x patches | 6.1.x | Aligned with Spring Boot 3.5.x |
| Spring for Apache Kafka | 3.3.10 | 3.3.x patches, 3.4 milestones | 3.2.x | Verify EOS compatibility modes |
| Apache Kafka (broker) | 4.1.0 | 4.1.x patches, 4.2 RC | 4.0.x | Validate upgrade in staging before promotion |
| Debezium | 3.3.0.Final | 3.3.x patches | 3.2.2.Final | Ensure connector compatibility with Kafka 4.1 |
| Apache Avro | 1.12.0 | 1.12.x patches | 1.11.4 | Shared schemas via `common-events-avro`; keep registry compatibility checks in CI |
| PostgreSQL | 18 | 18.x patches | 17 | Test pg_upgrade paths before production rollout |
| Temporal | 1.25.x (self-hosted) | Managed service evaluation | N/A | Decide in Phase 3 ADR |
| Istio | 1.24.x ambient | 1.25/1.26 | N/A | Track ambient multicluster features |
| Redis | 7.x | Managed options | N/A | Standardize TLS/auth config |
| Vault | 1.15.x | 1.16 RC | N/A | Evaluate enterprise vs OSS; ensure auto-unseal |
| CDN | TBD | Provider evaluation | N/A | Complete Phase 3 assessment |

## Shared Module Dependencies

| Module | Library | Version | Purpose |
|--------|---------|---------|---------|
| common-kafka | spring-kafka | 3.3.10 | Transactional producer/consumer support |
| common-kafka | kafka-clients | 4.1.0 | Exactly-once semantics, idempotent producers |
| common-persistence | spring-data-jpa | 3.5.6 | JPA entity management |
| common-persistence | hibernate-core | 6.6.1 | ORM implementation |
| common-persistence | flyway-core | 11.14.0 | Database migration management |
| common-sagas | spring-data-jpa | 3.5.6 | Saga state persistence |
| common-events-avro | avro | 1.12.0 | Avro schema serialization |
| common-proto | protobuf-java | 3.25.5 | Protocol buffer serialization |
| common-proto | grpc-stub | 1.68.0 | gRPC service stubs |
| common-temporal | temporal-sdk | 1.25.0 | Workflow orchestration |
| common-temporal | temporal-spring-boot-starter | 4.0.0 | Spring integration for Temporal |

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
| Kafka Brokers | 4.1.0 | infra/compose.yml | Event streaming backbone |
| Schema Registry | 7.7.0 | infra/compose.yml | Avro schema management |
| PostgreSQL | 18 | infra/compose.yml | Primary data store |
| Debezium Connect | 3.3.0 | infra/compose.yml | CDC and outbox event router |
| AKHQ | 0.24.0 | infra/compose.yml | Kafka management UI |
| Redis | 7-alpine | infra/compose.yml | Caching layer |
| Prometheus | 2.54.0 | infra/prometheus/prometheus.yml | Metrics collection |
| Grafana | 11.2.0 | infra/grafana/provisioning/ | Monitoring dashboards |

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
