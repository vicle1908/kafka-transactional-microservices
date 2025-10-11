# Phase 4 – Service Implementations

## Objectives

Note: This document is kept in sync with IMPLEMENTATION_PLAN.md and AGENTS.md.

- Build domain services following the shared template and transactional patterns.
- Implement saga workflows across Order, Payment, Inventory, and Notification services.
- Ensure thorough testing (unit, integration, contract) for each service.

## Deliverables

- Service modules: `orders-service`, `payments-service`, `inventory-service`, `notification-service`.
- Saga pilot documentation with state machine diagrams and code samples.
- Contract tests covering event schemas and downstream expectations.
- Saga metrics instrumentation via `common-sagas/SagaMetricsRecorder` plus dashboard placeholders.

## Task Board

|| ID | Task | Owner | Status | Notes |
||----|------|-------|--------|-------|
|| P4.1 | Scaffold `orders-service` with create order command + outbox emission | Platform Team | Completed | Command validation, Kotlinx payload serialization, and outbox persistence shipped with tests. Saga state is now started in `OrderService`. |
|| P4.2 | Implement `payments-service` consumer and payment processing | Platform Team | Completed | Payment handler routes through in-memory/HTTP gateway adapters, emits `PaymentCompleted`/`PaymentFailed`, and persists processed-event ledgers within the same transaction. Refund compensation records refund ledgers and emits `PaymentRefunded` outbox events with integration tests. Temporal interceptors aligned to OpenTracing bridge. |
|| P4.3 | Implement `inventory-service` reservations and adjustments | Platform Team | Completed | Reservation domain, processed-event ledger, pessimistic-locked stock ledger, saga metrics assertions, REST + gRPC (`StockReconciliationService`) in place. External gRPC consumers to be handled next phase. |
|| P4.4 | Implement `notification-service` with retry/backoff | Platform Team | Completed | Transactional notification service with retry template, channel senders (Email/SMS/Push), processed-event ledger, and tests. Timestamp alignment and formatting tracked/resolved; any remaining polish continues in later phases. |
|| P4.5 | Develop saga state persistence module (`sagas` table + repository) | Platform Team | Completed | `common-sagas` ships entity, repository, service, and Flyway migration. |
|| P4.6 | Create compensating action handlers for failed saga steps | Platform Team | Completed | Payment refund and inventory release placeholders with saga markers and regression tests; notification failure path increments metrics. |
|| P4.8 | Write unit and integration tests (Testcontainers) for each service | Platform Team | Moved to Phase 5 | Saga-aware tests exist; expand e2e and compensation cases tracked as P5.13. |
|| P4.9 | Build contract tests for event schemas | Platform Team | Completed | Avro schema contract tests (`common-events-avro/src/test/kotlin/com/example/events/SchemaContractTest.kt`). |
|| P4.10 | Introduce gRPC interfaces for synchronous coordination | Platform Team | Moved to Phase 5 | Inventory gRPC server implemented with tests; external consumer integrations tracked as P5.14. |
|| P4.11 | Document saga pilot in `docs/sagas/order-fulfillment.md` | Platform Team | Completed | Mermaid sequence diagram and failure-path notes done; Temporal pilot write-up moved to Phase 5. |
|| P4.12 | Create saga metrics dashboard documentation | Platform Team | Completed | `docs/runbooks/saga-dashboard.md` with metrics definitions, layout, alerting; implementation scheduled in Phase 5. |
|| P4.DB1 | Commit Flyway migrations for outbox/processed_events/sagas + service tables | Platform Team | Moved to Phase 5 | Per-service migrations tracked as P5.DB1. |
|| P4.DB2 | Fix service `application.yml` DB/Flyway/Kafka configs (payments, inventory) | Platform Team | Completed | YAML corrections (`hibernate.jdbc.time_zone`) and Flyway flags validated. |
|| P4.DB3 | Verify multi-DB compose + Debezium publishes outbox rows | Platform Team | Moved to Phase 5 | Tracked as P5.DB3. See Debezium runbook. |

## Research & References

- Saga design patterns (choreography vs orchestration)
- Spring Boot transactional messaging samples
- Testcontainers best practices for Kafka/Postgres

## Risks & Mitigations

- **Business logic drift**: Collaborate with domain experts for acceptance.
- **Idempotency bugs**: Add processed-event ledger tests.
- **Metrics blind spots**: Grafana dashboard documentation completed; track implementation in Phase 5 backlog and alert on counter growth anomalies once dashboards exist.

## Dependencies

- Shared modules from Phase 2.
- Outbox tooling and schema checks from Phase 3.

## Artifacts & Links

- Services: `services/*`
- Saga docs: `docs/sagas/order-fulfillment.md`, `docs/sagas/temporal-pilot.md`
- Avro contract tests: `common-events-avro/src/test/kotlin/com/example/events/SchemaContractTest.kt`
- Shared modules: `common-kafka`, `common-sagas`, `common-proto`, `common-temporal`, `common-outbox-relay`, `common-observability`
- Debezium runbook: `docs/runbooks/debezium.md`

## Acceptance & Validation

- Links validated against repository paths:
  - Saga docs: `docs/sagas/order-fulfillment.md`, `docs/sagas/temporal-pilot.md`
  - Avro contract tests: `common-events-avro/src/test/kotlin/com/example/events/SchemaContractTest.kt`
  - Shared modules referenced: `common-kafka`, `common-sagas`, `common-proto`, `common-temporal`, `common-outbox-relay`, `common-observability`
  - Debezium runbook: `docs/runbooks/debezium.md`
- Kafka transactional consumer configuration:
  - `common-kafka` uses `containerProperties.kafkaAwareTransactionManager` (preferred in Spring Kafka 3.2+).
  - `services/notification-service/.../NotificationKafkaConfig.kt` currently sets `containerProperties.transactionManager`; follow-up created as P5.KAFKA1 to align.
- Flyway enabled across services via `application.yml` with `ddl-auto=validate` and `baseline-on-migrate=true`; per-service migration SQLs tracked in Phase 5 (P5.DB1).
- Temporal pilot present in `temporal-pilot` with `OrderFulfillmentWorkflowImpl`; remaining worker wiring/tests moved to Phase 5 (P5.12).
- gRPC server present in inventory: `services/inventory-service/.../adapter/inbound/grpc/StockReconciliationGrpcService.kt` (with tests);
  external consumer integration moved to Phase 5 (P5.14).
- Debezium verification moved to Phase 5 (P5.DB3); operational steps documented in `docs/runbooks/debezium.md`.

## Progress Log

- 2025-10-08 | Added gRPC task for synchronous coordination; clarified schema contract testing plan.
- 2025-10-08 | Initialized `orders-service` module with Gradle configuration, command DTO, and baseline migration (P4.1 In Progress).
- 2025-10-08 | Created `payments-service` module with transactional command handler and shared outbox integration; updated board statuses.
- 2025-10-08 | Added processed-event ledger, transactional Kafka listener, and Embedded Kafka tests for `payments-service` (P4.2 progress).
- 2025-10-09 | Brought `inventory-service` to parity with domain schema, transactional reservation service, `InventoryReservedEvent` contract, and Embedded Kafka listener tests (P4.3 in progress).
- 2025-10-09 | Scaffolded `notification-service` with domain schema, transactional notification sender, listener for `InventoryReservedEvent`, and contract tests (P4.4 in progress).
- 2025-10-09 | Fixed ktlint violations in `notification-service` and verified `./gradlew clean check` passes across modules.
- 2025-10-09 | Added `common-sagas` shared module with saga state entity, optimistic transition service, Flyway migration, and DataJpa tests (P4.5 complete).
- 2025-10-09 | Wired saga state transitions through Orders → Payments → Inventory → Notification services with new tests exercising the shared ledger (P4.2–P4.4 updates).
- 2025-10-09 | Introduced `SagaMetricsRecorder`, instrumented all services/tests with counter assertions, and drafted dashboard placeholders in docs (P4.2–P4.4).
- 2025-10-09 | Payment service now charges via gateway adapter, emits `PaymentFailedEvent` on declines, and records processed-event ledgers inside the transactional unit (P4.2 progress).
- 2025-10-09 | Added HTTP payment gateway adapter with configurable timeouts/retries, MockWebServer tests, and documented property layout for switching modes (P4.2 progress).
- 2025-10-09 | Inventory service now enforces stock availability via pessimistic-locked `inventory_stock` table with Flyway migration and failure-path tests (P4.3 progress).
- 2025-10-09 | Notification service adds email, SMS, and push senders governed by channel properties with retry/failure simulation hooks; tests cover success + failure paths (P4.4 progress).
- 2025-10-09 | Added Avro schema contract regression tests and created `common-proto` module with stock reconciliation proto definitions (P4.9 complete, P4.10 in progress).
- 2025-10-09 | Documented Temporal pilot workflow/activities plan in `docs/sagas/temporal-pilot.md` and added saga sequence diagram (P4.7/P4.11 progress).
- 2025-10-09 | Delivered inventory-service gRPC adapter with Netty server lifecycle, property-driven port configuration, and in-process tests covering success, validation, and failure scenarios (P4.3 progress, P4.10 progress).
- 2025-10-09 | Implemented payments-service refund compensation: new refund ledger/table, `PaymentRefundedEvent` schema, transactional outbox emission, and idempotency coverage in integration tests (P4.2 progress).
- 2025-10-09 | Completed saga dashboard documentation with metrics definitions, layout, and alerting rules in `docs/runbooks/saga-dashboard.md` (P4.12 complete).
- 2025-10-10 | Replaced unsupported Temporal OpenTelemetry dependency with the OpenTracing bridge, ensuring worker/client interceptors load successfully (P4.2/P4.7 progress).
- 2025-10-10 | Introduced CLI-based Detekt workflow plus baseline (`detektAll`, `detektBaseline`) to keep static analysis running on Kotlin 2.2; documented follow-up to burn down baseline findings (cross-cutting).
- 2025-10-10 | Aligned outbox schema/ORM mappings on `occurred_at`, re-ran Flyway migrations, and restored `common-persistence` repository tests (P4.2–P4.4 underpinning).
- 2025-10-10 | Began notification/inventory ktlint remediation; remaining formatting fixes (client listener + raw-string payloads) tracked as blockers before marking P4.3/P4.4 complete.
- 2025-10-10 | Started repository-wide ktlint cleanup (notification + orders services now formatted); payments-service, additional adapters, and residual detekt suppressions remain before `./gradlew clean check` turns green (P4.2–P4.4 follow-up).
- 2025-10-10 | Moved P4.7 Temporal pilot to Phase 5; normalized Task Board formatting (single-pipe) and updated statuses (P4.2–P4.4, P4.DB2 → Completed).
- 2025-10-10 | Moved remaining Phase 4 items to Phase 5: P4.8 (tests → P5.13), P4.10 (gRPC consumers → P5.14), P4.DB1 (migrations → P5.DB1), P4.DB3 (Debezium verification → P5.DB3).
