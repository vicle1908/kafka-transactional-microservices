# Technical Decision Record – October 2025 Baseline

## Platform Stack
- **Application Framework**: Spring Boot 3.5.6 (Spring Framework 6.2.11) on Java 25 runtime (fallback to Java 23/21 where required) for the latest GA improvements and security updates.
- **Primary Language**: Kotlin 2.2.20 for service implementations, templates, and shared libraries; Java remains available for interoperability and JVM tooling.
- **Messaging Backbone**: Apache Kafka 4.1.0 (KRaft) providing log-based event streaming, transactional producers/consumers, and queue preview features.
- **Change Data Capture**: Debezium 3.3.0.Final with Outbox Event Router SMT to publish database outbox entries into Kafka with exactly-once semantics.
- **Build & Tooling**: Gradle 9.1.0 (Kotlin DSL, version catalog), Testcontainers 1.20+, Docker Engine 27+, OpenTelemetry SDK 1.44+ for consistent builds and observability.
- **Code Quality**: ktlint Gradle plugin 13.1.0 (CLI 1.7.1), Detekt 1.23.8 with formatting integration, Jacoco coverage reports wired to `check`.
- **Event Serialization**: Apache Avro with Schema Registry (Confluent/Apicurio) as the canonical contract format for transactional outbox events; Debezium connectors emit Avro payloads via `BinaryDataConverter`. New contracts such as `PaymentCompletedEvent` and `InventoryReservedEvent` codify cross-service state transitions.
- **JSON Handling**: Remain on Jackson 2.20.0 (latest GA in the 2.x line) with Spring Boot 3.5.x while Boot 4.0 milestones stabilize Jackson 3 support; Kotlinx Serialization shapes nested JSON fragments embedded in Avro event payloads.

## Connectivity Strategy
- **North–South Traffic**: Spring Cloud Gateway remains the API gateway for authentication, rate limiting, protocol adaptation, and request shaping.
- **East–West Traffic**: Istio (ambient mode, 1.24+) provides service-mesh capabilities—mTLS, traffic policy, observability—without per-pod sidecars. Gateway routes terminate at mesh ingress; ambient waypoints enforce namespace-level policies.
- **Multi-Cluster Readiness**: Track Istio ambient multicluster (1.27+) to plan future active-active failover scenarios.

## Data Consistency & Workflow Coordination
- **Transactional Outbox**: Services persist domain changes and outbox entries in one transaction; Debezium replicates to Kafka.
- **Processed Event Ledger**: Each service owns a `processed_events` table keyed by message UUID to provide idempotent guards for Kafka listeners participating in transactional sessions.
- **Saga Pattern**: Use Temporal for complex, long-running workflows requiring orchestration, retries, and compensation transparency; retain simple event-choreographed sagas for lightweight interactions.

## Operational Guardrails
- Maintain `docs/version-matrix.md` listing current, candidate, and fallback versions; enforce via Gradle `versionCheck` in CI.
- Integrate `schemaCompatibilityCheck` into pipelines to prevent breaking event contracts.
- Document runbooks for API gateway, service mesh, Debezium connectors, Vault secrets, and polyglot datastores.
- Instrument saga flows with `SagaMetricsRecorder` counters (`saga.step.processed`) and publish Grafana dashboards during Phase 5 before production rollout.

## Justification
- Kotlin improves developer productivity, null safety, and interoperability with Spring while retaining JVM compatibility.
- ktlint and Detekt enforce consistent Kotlin style and static analysis across services, while Jacoco reports guard coverage regressions.
- Latest stable releases deliver security fixes and features while the version matrix + CI gates limit upgrade risk.
- Deferring Jackson 3 until Spring Boot 4 reaches GA follows Spring’s migration guidance (Oct 7 2025 blog) and avoids exposing production code to milestone regressions.
- Service mesh + gateway separation allows zero-trust east–west policies without overloading the edge gateway.
- Temporal orchestration reduces duplicated compensation logic versus pure choreography and increases observability of sagas.
- Debezium outbox eliminates dual-write race conditions that a pure Kafka client approach cannot solve alone.

## References
- Spring Boot 3.5.6 release announcement (Sept 18, 2025).
- Apache Kafka 4.1.0 release notes (Sept 2, 2025).
- Debezium 3.3.0.Final release notes (Oct 1, 2025).
- Istio ambient mode roadmap (Istio 1.24+).
- Temporal saga best-practice guides (Aug 2025).
- Kotlin for Spring Boot adoption reports (JetBrains 2025).
