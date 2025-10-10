# Phase 0 Tech Stack Confirmation

## Core Runtime
- Java 25 (OpenJDK) as default runtime; Java 23 LTS retained as first fallback and Java 21 available for compatibility testing.
- Spring Boot 3.5.6 / Spring Framework 6.2.11.

## Messaging & CDC
- Apache Kafka 4.1.0 (KRaft mode) with Schema Registry.
- Debezium 3.3.0.Final for transactional outbox replication.

## Connectivity
- Spring Cloud Gateway for north–south ingress.
- Istio 1.24+ ambient mesh for east–west security and observability.

## Workflow & Persistence
- Temporal for orchestrated sagas, Redis for cache-aside patterns, PostgreSQL 18 for transactional stores.

## Observability & DevOps
- OpenTelemetry SDK 1.44+, Prometheus/Grafana, Jaeger/Tempo tracing.
- Gradle 9.1.0 (Kotlin DSL), Testcontainers 1.20+, Terraform/Helm IaC, GitHub Actions CI/CD.

## References
- `docs/version-matrix.md` tracks current/candidate/fallback versions.
- ADRs under `docs/adrs/` detail decisions and trade-offs.
