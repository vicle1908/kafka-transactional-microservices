# Phase 4 – Service Implementations

## Objectives
- Build domain services following the shared template and transactional patterns.
- Implement saga workflows across Order, Payment, Inventory, and Notification services.
- Ensure thorough testing (unit, integration, contract) for each service.

## Deliverables
- Service modules: `orders-service`, `payments-service`, `inventory-service`, `notification-service`.
- Saga pilot documentation with state machine diagrams and code samples.
- Contract tests covering event schemas and downstream expectations.
- Saga metrics instrumentation via `common-sagas/SagaMetricsRecorder` plus dashboard placeholders.

## Task Board
| ID | Task | Owner | Status | Notes |
|----|------|-------|--------|-------|
| P4.1 | Scaffold `orders-service` with create order command + outbox emission | Platform Team | Completed | Command validation, Kotlinx payload serialization, and outbox persistence shipped with tests. Saga state is now started in `OrderService`. |
| P4.2 | Implement `payments-service` consumer and payment processing | Platform Team | In Progress | Payment handler routes through configurable in-memory/HTTP gateway adapters, issues `PaymentCompleted`/`PaymentFailed` events, and persists processed-event ledgers within the same transaction. Refund compensation now records refund ledgers, emits `PaymentRefunded` outbox events, and ships integration tests. Temporal client interceptors have been aligned to the supported OpenTracing bridge; external provider integration and refund gateway stubs are next. |
| P4.3 | Implement `inventory-service` reservations and adjustments | Platform Team | In Progress | Reservation domain, processed-event ledger, pessimistic-locked stock ledger, and saga metrics assertions in tests are complete. Stock reconciliation REST API and gRPC adapter (via `StockReconciliationService`) now expose adjustments to synchronous clients. Outbox schema alignment (`occurred_at`) and ktlint clean-up are underway; external gRPC consumers pending. |
| P4.4 | Implement `notification-service` with retry/backoff | Platform Team | In Progress | Transactional notification service, failure retry template, saga metrics recorder usage, and embedded tests in place. Email/SMS/Push sender adapters now registered with configurable failure simulation. Outbox event timestamp alignment and ktlint formatting of client/tests remain outstanding before closing. |
| P4.5 | Develop saga state persistence module (`sagas` table + repository) | Platform Team | Completed | `common-sagas` module ships shared entity, repository, service, and Flyway migration. |
| P4.6 | Create compensating action handlers for failed saga steps | Platform Team | Completed | Added payment refund and inventory release placeholders with saga markers and regression tests; notification failure path increments metrics. |
| P4.7 | Implement Temporal workflows/activities for orchestrated saga pilot | Platform Team | In Progress | Integrate `temporal-spring-boot-starter-kotlin`. Implement `OrderFulfillmentWorkflow` and activities using annotations. Configure workers in `application.yml`. Write integration tests with `TestWorkflowEnvironment`. |
| P4.8 | Write unit and integration tests (Testcontainers) for each service | Platform Team | In Progress | Added saga-aware tests in Orders/Payments/Inventory/Notification services; coverage to expand with compensations and gRPC adapters. |
| P4.9 | Build contract tests for event schemas | Platform Team | Completed | Added Avro schema contract tests (`SchemaContractTest`) in `common-events-avro`. |
| P4.10 | Introduce gRPC interfaces for synchronous coordination | Platform Team | In Progress | `common-proto` module introduced with shared stock reconciliation proto definitions; service implementation will hook in once consumers are ready. |
| P4.11 | Document saga pilot in `docs/sagas/order-payment-inventory.md` | Platform Team | In Progress | Added mermaid sequence diagram and expanded failure-path notes; Temporal pilot write-up pending. |
| P4.12 | Create saga metrics dashboard documentation | Platform Team | Completed | Created `docs/runbooks/saga-dashboard.md` with metrics definitions, dashboard layout, and alerting rules. Implementation will be in Phase 5. |
| P4.DB1 | Commit Flyway migrations for outbox/processed_events/sagas + service tables | Platform Team | In Progress | Per-service under `src/main/resources/db/migration`. |
| P4.DB2 | Fix service `application.yml` DB/Flyway/Kafka configs (payments, inventory) | Platform Team | Planned | Correct YAML + `hibernate.jdbc.time_zone` nesting. |
| P4.DB3 | Verify multi-DB compose + Debezium publishes outbox rows | Platform Team | Planned | Create order; observe topics per connector. |

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
- Service source directories (`services/*`)
- Saga documentation (`docs/sagas/`)
- Contract test suites (`contracts/*`)

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
