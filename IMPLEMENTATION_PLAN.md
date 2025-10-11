# Kafka Transactional Microservices – Implementation Plan

## 1. Objectives

- Deliver Kafka-backed microservices that guarantee atomic business state updates and message publication.
- Standardize on the transactional outbox pattern with Debezium-based relays for cross-service messaging with exactly-once semantics.
- Provide observability, resiliency, and operational runbooks to support production deployment.
- Implement comprehensive integration testing to validate end-to-end transactional guarantees.

## 2. Phased Roadmap

### Phase 0 – Discovery & Architecture (Week 1)

- Confirm candidate microservices (Order, Payment, Inventory, Notification) and their datastores.
- Map critical flows requiring exactly-once vs at-least-once guarantees and justify complexity requirements.
- Finalize tech stack: Spring Boot 3.5.6, Kotlin 2.2.20 on Java 25 (fallback to Java 23/21 where required), Kafka 4.1.0, PostgreSQL 18, Debezium 3.3.0.Final.
- Draft ADRs covering transactional outbox selection, saga style (choreography), and schema governance.
- Define broker configuration requirements for EOS: `transaction.state.log.replication.factor >= 3`,`transaction.state.log.min.isr >= 2`.
- Execution board: [PHASE-0](docs/phases/PHASE-0.md)

### Phase 1 – Platform Foundation (Weeks 2-3)

- Provision local and shared Kafka clusters with pure KRaft metadata mode (no ZooKeeper),Schema Registry and AKHQ/Kafdrop.
- Configure brokers for transactions with proper replication (`min.insync.replicas >= 2`, transaction logs, idempotence defaults).
- Set up Docker Compose for local infra under `infra/compose.yml` (Kafka, Postgres, Debezium, Schema Registry,Redis for caching).
- Configure Postgres for Debezium logical replication and multi-database:
  - Enable `wal_level=logical`, `max_wal_senders`, `max_replication_slots` via `postgres` command flags.
- Created `infra/postgres/init/01-create-databases.sql` to create `payments`, `inventory`, and `notifications` databases owned by `app`. Standardized database naming to use plural `notifications` across infra and services.
  - Create a dedicated replication user `debezium` with LOGIN/REPLICATION privileges.
  - Document bootstrap steps in `docs/runbooks/debezium.md`.
- ✅ **COMPLETED**: **PHASE 1 CI/CD HARDENING** - Comprehensive GitHub Actions security and performance enhancement:
  - **Security Framework**: Explicit permissions blocks, Gradle wrapper validation, pinned actions, concurrency controls
  - **Performance Optimization**: gradle/actions/setup-gradle@v4 integration, configuration cache, parallel linting execution  
  - **Quality Assurance**: Fixed all detekt/ktlint violations, Java version check supports 21-25, workflow YAML validated
  - **Workflow Coverage**: Hardened CI (ci.yml), nightly integration tests (integration-test.yml), secure container publishing
  - **Automation**: Branch protection and post-merge setup scripts, comprehensive monitoring documentation
  - **Status**: ✅ PR #1 ready for merge - all quality gates passing
- Deploy Istio (ambient profile) in non-prod clusters;configure Gateway API integration with Spring Cloud Gateway at the edge.
- **COMPLETED**: Implement comprehensive health checks for all services in Docker Compose files.
- Execution board: [PHASE-1](docs/phases/PHASE-1.md)

### Phase 2 – Service Template & Shared Components (Weeks 3-4)

- Create Gradle multi-module baseline: `common-events`, `common-kafka`, `common-persistence`, `common-observability`.
- Create `common-temporal` module for shared workflow interfaces, activities, and DTOs.
- Database baseline with Flyway per service:
  - Canonicalize `outbox` schema (snake_case): `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `headers?`, `occurred_at`, `published_at?`.
  - Add `processed_events` table (consumers): `event_id` (PK), `processed_at`.
  - Add `sagas` table using the `SagaStateEntity` shape (single canonical entity); remove/replace alternative mappings.
  - Add `inventory_items` (and reservations if used) with pessimistic locking support.
  - Place migrations under each service at `src/main/resources/db/migration` and enable Flyway (`spring.flyway.enabled=true`).
  - 2025-10-11: Completed baseline migrations for orders, payments, inventory, and notification services; added `CREATE EXTENSION IF NOT EXISTS pgcrypto;` to support UUID defaults in local dev; aligned `notifications` DB naming across infra and Flyway.
- Wire `KafkaTransactionManager`, transactional `KafkaTemplate`, and error handling interceptors with proper `transactional.id` configuration.
- Configure Kafka producers with `enable.idempotence=true`, `acks=all`, and unique `transactional.id`.
- Configure Kafka consumers with `isolation.level=read_committed` and `enable.auto.commit=false` for manual offset management.
- Package Debezium connector configuration templates (JSON) with Outbox Event Router SMT.
- Ensure each service `application.yml` declares Datasource/JPA/Flyway/Kafka sections and points to its database (orders, payments, inventory, notifications).
- Create `docs/version-matrix.md` and add a shared Gradle `versionCheck` task to enforce runtime compatibility across modules (Java 25 baseline with fallbacks to Java 23/21, Spring Boot 3.5.x, Kafka 4.1.x, Debezium 3.3.x).
- Stand up the internalcode-quality toolchain: `ktlint` (format + lint), Detekt, Jacoco reports, `.editorconfig`, and CI wiring (`./gradlew check`).
- Document local dev workflows in `docs/dev/getting-started.md`.
- Execution board: [PHASE-2](docs/phases/PHASE-2.md)

### Phase 3 – Outbox Relay & Tooling (Week 4)

- Spike polling relay vs Debezium CDC: measure latency, failure recovery, ops overhead, and justify the choice for operational complexity.
- Implement message relay with proper transaction boundaries: Begin Kafka transaction → Process outbox records→ Commit Kafka transaction → Update DB status.
- Adopt Debezium as default, retain lightweight poller for services without CDC (feature flagged).
- Normalize Debezium connector JSONs under `infra/debezium/connectors/`:
  - Set `publication.autocreate.mode=filtered` and unique `slot.name` per DB.
  - Use `transforms.outbox.table.field.event.key=aggregate_id` and route by `aggregate_type`.
  - Use Avro value converter with Schema Registry; keep String key converter.
  - Remove/retire legacy `infra/debezium/outbox-connector.json` that uses camelCase/BinaryDataConverter.
- Build replay tooling (`scripts/outbox-replay.sh`) to re-emit outbox rows by `event_id` or time range.
- Add monitoring dashboards for Debezium lag, connector health, transaction aborts.
- Implement metrics for `outbox_table_depth`, `relay_kafka_commit_latency`, and `end_to_end_latency` in Grafana dashboards under `infra/grafana/`.
- Introduce shared Avro schema module (`common-events-avro`) and registerschemas via Schema Registry clients.
- Document Debezium connector operations runbook and replay procedure under `docs/runbooks/debezium.md`.
- Document schema registry publication process and helper scripts under `docs/runbooks/schema-registry.md` with `scripts/schema-publish.sh`.
- Integrate schema compatibility checks (`./gradlew schemaCompatibilityCheck`) into CI alongside `ktlintCheck`, `detekt`, `spotbugsMain`, `spotbugsTest`, and ErrorProne gates.
- Evaluate Temporal workflow platform (self-hosted vs managed) for saga orchestration; capture decision in ADR.
- **COMPLETED**: Implemented polling relay mechanism as fallback to Debezium with scheduled processing, REST API endpoints, metrics collection, and health indicators. Documented in ADR 0005 and runbooks.
- Execution board: [PHASE-3](docs/phases/PHASE-3.md)

### Phase 4 – Service Implementations (Weeks 5-8)

- Iteratively enable services following template:
  - `orders-service`: order creation, outbox emission, compensation hooks, saga state kickoff.
  - `payments-service`:consume OrderCreated, process payment, emit PaymentCompleted/Failed, append saga transitions.
  - `inventory-service`: reserve stock, maintain idempotency ledger, advance saga state.
  - `notification-service`: consume events, send emails/SMS via external providers with retry, finalize saga or mark failure.
- Database acceptance for each service:
  - Commit Flyway migrations for `outbox`, `processed_events`, `sagas` (if local), and service-specific tables (e.g., `inventory_items`).
  - Ensure `spring.jpa.hibernate.ddl-auto=validate` and `spring.flyway.enabled=true` so schema is applied by migrations only.
  - Fix `application.yml` datasource/JPA/Flyway/Kafka sections for `payments-service` and `inventory-service`; correct YAML/indentation issues (e.g., `hibernate.jdbc.time_zone`).
  - Verify Debezium connectors publish outbox rows by creating an order and observing topics.
- Embed JSONfragments for Avro payloads via Kotlinx Serialization (`Json.Default`) and validate create-order commands (non-blank customer, non-empty items) before persistence; retain envelope tests that assert nested payload structure and failure paths.
- Extend payments implementation with an idempotent Kafka consumer (Spring Kafka + `KafkaTransactionManager`) thatpersists a `processed_events` ledger before committing offsets; integration tests rely on Testcontainers to verify one-and-only-once semantics with full Kafka/DB environment.
- Stand up inventory reservations by consuming `PaymentCompletedEvent`, reserving stock with a transactional `InventoryService`, and publishing `InventoryReservedEvent` payloads through theoutbox; guard duplicate delivery via the shared processed-events ledger and Testcontainers tests.
- Build notification delivery by consuming `InventoryReservedEvent`, persisting notification records, and emitting `NotificationSentEvent`; integrate processed-event guard rails and capture provider payloads for downstream retries.
- Introduce shared saga persistence (`common-sagas`) withFlyway migration, JPA entities, repositories, and a `SagaStateService` that coordinates optimistic transitions.
- Add compensating-action scaffolding in payments (`PaymentService.compensate`) and inventory (`InventoryService.release`) to mark sagas as `COMPENSATING`/`FAILED` while downstream integration work is plannedfor Phase 5.
- Maintain service template guidance in `docs/dev/service-template.md`; new modules should follow hexagonal slices and depend on shared components.
- Introduce gRPC endpoints (Protobuf contracts) only for synchronous coordination paths that cannot be event-driven; map gRPC DTOs to Avro events within application servicesand share IDLs via a dedicated `common-proto` module.
- Write component and contract tests (Spring Boot Test + Testcontainers for Kafka/Postgres).
- Implement comprehensive end-to-end integration tests in GitHub Actions: API call → DB write → outbox insert → relay processing → Kafka publish → consumer processing validation.
- Establish saga workflows (Order → Payment → Inventory) with compensating events.
- Deliver a saga pilot implementation documenting state machine persistence, compensation handlers, and idempotent consumers in `docs/sagas/`.
- **Temporal Saga Pilot**:
  - Refactor to granular, single-purpose Activity interfaces (`PaymentActivity`, `InventoryActivity`) in `common-temporal`.
  - Designate a dedicated workflow worker service (from `temporal-pilot`) to host the `OrderFulfillmentWorkflow`.
  - Implement and configure activity workers in each respective microservice (`payments-service`, `inventory-service`, etc.).
  - Update integrationtests to focus on workflow invocation in `orders-service`.
- Execution board: [PHASE-4](docs/phases/PHASE-4.md)

### Phase 5 – Observability & Resilience (Weeks 7-9)

- Integrate OpenTelemetry for tracing across HTTP/Kafka boundaries; propagatecontext headers.
- Configure Micrometer metrics exporters for Kafka transactions, outbox lag, consumer lag, DLQ counts.
- **Temporal Observability**:
  - Configure the Temporal SDK to export Micrometer metrics to Prometheus.
  - Add the OpenTelemetry tracing interceptor to Temporal workers to ensure end-to-end tracepropagation through sagas.
  - Create a Grafana dashboard for key Temporal metrics (e.g., workflow latency, activity failures, retry rates).
- Implement monitoring for `outbox_table_depth` with alerting to detect relay service failures.
- Implement retry strategies (Spring Retry, DLQ topics) and chaos drills (broker restart, DB failover) via GitHub Actions workflow (chaos-engineering.yml).
- Document runbooks in `docs/runbooks/` for connectors, DLQ reprocessing, and saga failure recovery.
- Complete API gateway, Debezium connector, and polyglot datastore runbooks referenced in @AGENTS.md; ensure automation scripts are version-controlled.
- **✅ COMPLETED**: Comprehensive observability and resilience implementation including:
  - **Enhanced OpenTelemetry Configuration**: Complete OpenTelemetry SDK integration with comprehensive tracing, metrics, and context propagation configuration
  - **Automatic Instrumentation**: AOP-based instrumentation for services, repositories, and Kafka operations with custom tracing aspects
  - **Structured Logging Implementation**: Created StructuredLogger utility in common-observability module with OpenTelemetry integration for consistent JSON-formatted log messages
  - **Service Integration**: Integrated StructuredLogger across all microservices with enhanced logging for business operations and trace context propagation
  - **Comprehensive Kibana Dashboard**: Created service logs dashboard with 9 visualizations including trace correlation, saga tracking, error pattern analysis, and operation performance monitoring
  - **Load Testing Enhancement**: Overhauled load testing workflow with configurable parameters and EOS validation
  - **ELK Stack Infrastructure**: Enhanced infrastructure configuration with complete ELK stack components (Elasticsearch, Logstash, Kibana, Filebeat)
  - **Runbook Suite**: Complete set of operational runbooks covering API gateway, service mesh, Debezium, polyglot datastores, Temporal, and OpenTelemetry
  - **Git Hooks Implementation**: Implemented Git hooks to enforce Gradle tasks before allowing commits and pushes, ensuring code quality is maintained locally
- **Execution Status**: ✅ **SUBSTANTIALLY COMPLETE** - Core observability infrastructure implemented with remaining tasks focused on deployment and validation
- Execution board: [PHASE-5](docs/phases/PHASE-5.md)

### Phase 6 – Hardening & Launch (Weeks 9-12)

- Conduct load tests simulating peak traffic with GitHub Actions workflow (load-test.yml); validate EOSbehavior under backpressure and high outbox table depth.
- Perform disaster recovery exercises (restore DB snapshot, rebuild Debezium connector offsets, replay outbox).
- Secure the platform (TLS/SASL, Kafka ACLs, secrets rotation) and complete compliance reviews.
- Run canary deployment for each service with feature flagsvia GitHub Actions workflow (canary-deployment.yml); monitor `outbox_table_depth` and transaction commit metrics before full rollout.
- Execution board: [PHASE-6](docs/phases/PHASE-6.md)

### Phase 7 – Service Mesh Integration (Istio) (Weeks 13-14)

- **Note**: This phase formalizes and completes the preliminary Istio setup initiated in Phase 1.
- **Objectives**:
  - Integrate Istio ambient mode as the service mesh for all east-west traffic, operating in a **hybrid model** with the existing Spring Cloud Gateway.
  - Utilize Istio for advanced traffic management and resilience for internal service-to-service communication.
  - Enforce a zero-trust security model within the mesh using Istio's security features.
  - Ensure seamless observability by integrating Istio telemetry with the project's existing monitoring stack.
- **Deliverables**:
  - An ADR (`docs/adrs/0006-istio-adoption.md`) formalizing the adoption of Istio.
  - Production-ready Infrastructure-as-Code (Helm/Terraform) for deploying and managing Istio.
  - `VirtualService`, `DestinationRule`, and `Gateway` resources for all services.
  - A global `PeerAuthentication` policy enforcing strict mTLS.
  - Granular `AuthorizationPolicy` resources for least-privilege service access.
  - An updated `canary-deployment.yml` workflow that uses Istio for traffic splitting.
  - Dedicated Grafana dashboards for monitoring service mesh health and performance.
- **Execution board**: [PHASE-7](docs/phases/PHASE-7.md)

- **Key Considerations**:
  - **Architecture**: This phase implements a hybrid gateway model. Spring Cloud Gateway is retained for its application-aware features at the edge, while Istio manages internal east-west traffic, providing a clear separation of concerns.
  - **Risk Mitigation**: The primary risks of this approach are potential latency overhead, operational complexity, and observability gaps. These are mitigated by specific tasks for latency benchmarking, using GitOps for unified configuration management, and ensuring end-to-end trace context propagation.
- **Milestones & Owners (target timeline)**:
  - **2025-10-13** – *Platform Team* (`P7.2`, `P7.3`): Run AGENTS research workflow (Context7 + DeepWiki) against existing Istio Helm/Terraform templates before authoring ambient-profile IaC and SCG ingress integration manifests; deliver draft Helm chart and Terraform outline.
  - **2025-10-15** – *Security Team* (`P7.5`, `P7.6`): Produce global `PeerAuthentication` and scoped `AuthorizationPolicy` definitions referenced in ADR 0006, validating least-privilege rules against sample service traffic matrices.
  - **2025-10-17** – *DevOps Team* (`P7.7`): Update `canary-deployment.yml` to drive Istio traffic shifting and mesh-aware smoke tests within CI; stage dry run in non-prod GitHub environment.
  - **2025-10-18** – *Observability Team* (`P7.8`, `P7.10`): Extend Grafana dashboards with Istio telemetry, verify SCG→Istio→service trace propagation via OpenTelemetry collector, and document procedures in `docs/runbooks/service-mesh.md`.
  - **2025-10-20** – *QA Team* (`P7.9`): Execute integration suite covering mTLS enforcement, failure injection, and fallback routing; record results and follow-ups on the PHASE-7 board.

## 3. Deliverables

- `@AGENTS.md`: living knowledge base for agents(complete).
- Discovery artifacts: workshop schedule/notes (`docs/notes/phase-0-*`), service catalog, ADRs 0001–0003.
- Service template repo modules with transactional scaffolding.
- Shared Avro schema module and registry configuration for event contracts.
- Sharedsaga persistence module (`common-sagas`) covering entity, repository, service, and migrations used by all services.
- Infrastructure code for Kafka/Postgres/Debezium/Redis deployments.
- Automated test suites (unit, integration, contract, load) per service with comprehensive EOS validation.
- Observability dashboards and alerting rules for outbox depth, relay performance, and transaction metrics.
- Operational runbooks (incident response, replay, DR).
- Redis caching blueprint and environment configuration.
- CDN/edge caching configuration with monitoring dashboards.
- **✅ COMPLETED**: Additional runbooks for polling relay mechanism and connector configuration guide.
- **✅ COMPLETED**: Complete runbook suite covering API gateway, service mesh, Debezium, polyglot datastores, Temporal, and OpenTelemetry operations.
- **✅ COMPLETED**: Grafana dashboards for Temporal, CDN metrics, outbox monitoring, and service performance.
- **✅ COMPLETED**: Centralized logging with complete ELK stack implementation (Elasticsearch, Logstash, Kibana, Filebeat).
- **✅ COMPLETED**: Complete OpenTelemetry implementation with enhanced configuration, automatic instrumentation, and comprehensive tracing across services.
- **✅ COMPLETED**: Structured logging implementation with StructuredLogger utility integrated across all microservices with OpenTelemetry trace context.
- **✅ COMPLETED**: Comprehensive Kibana dashboard configuration with 9 visualizations for service log monitoring, trace correlation, and saga tracking.
- **✅ COMPLETED**: Load testing workflow overhaul with configurable parameters and enhanced EOS validation.
- **✅ COMPLETED**: Git hooks implementation for local code quality enforcement.
- **✅ COMPLETED** *(2025-10-11)*: Docker/Compose infrastructure image audit aligning environments to latest stable tags (Schema Registry 8.0.1, PostgreSQL 18.0, Redis 8.2.2-alpine, Debezium Connect 3.3.0.Final, Elastic Stack 8.18.8, Prometheus 3.6.0, Grafana 12.2.0, OpenTelemetry Collector 0.137.0, Jaeger 1.74.0, Flyway 11.13.2).

## 4. Open Decisions & Research Tasks

-Finalize choice between Debezium connectors vs lightweight polling for low-volume services based on operational complexity assessment.
- Evaluate Confluent vs open-source Kafka distribution for licensing & support.
- Decide on schema format (Avro vs JSON Schema) and registry enforcement rules.
- Confirm saga coordinator needs for complex workflows (e.g.,orchestrator vs choreography).
- Assess infrastructure hosting (self-managed Kubernetes vs managed Kafka services such as MSK, Confluent Cloud).
- Select CDN provider and deployment model (managed vs self-hosted edge).
- Determine Temporal deployment option (self-hosted vs managed service).
- **(RESOLVED)** Periodically reassess service meshchoice (Istio ambient vs Linkerd/managed meshes) based on resource footprint, cost, and feature needs. **Decision**: Adopt Istio ambient mode. See [ADR 0006](docs/adrs/0006-istio-adoption.md).
- Evaluate secrets management deployment (Vault OSS vs enterprise vs cloud-native secret stores).
- Select Istio waypoint deployment strategy (per-namespace vs per-service) for Spring Cloud Gateway ingress; document trade-offs in ADR 0006 update.

## 5. Risks & Mitigations

- **Debezium lag or outages**: Implement alerting,auto-restart scripts, and replay tooling.
- **EOS configuration drift**: Enforce shared Spring Kafka config via `common-kafka` module; add integration tests with full environment validation.
- **Schema incompatibilities**: Automate schema validation in CI; require backward-compatible changes.
- **Operational complexity of relay service**: Provide detailed runbooks, offer fallback poller mode, schedule regular drills, implement comprehensive monitoring for `outbox_table_depth`.
- **Performance bottlenecks**: Monitor outbox table depth and implement proper indexing; optimize relay processing batch sizes.
- **Security gaps**: Apply TLS/SASL, integrate secrets vault, conductthreat modeling sessions.
- **Observability gaps**: Implement centralized logging and complete OpenTelemetry deployment to ensure full visibility across all services.

## 6. Next Actions (Current Phase)

### 🚀 **Phase 7 Kickoff (Week 13 starting 2025-10-13)**
- `P7.2`/`P7.3` Platform Team (due 2025-10-14): Complete AGENTS research loop (Context7 Istio docs, DeepWiki repo scan) and draft Helm/Terraform ambient-profile modules plus Spring Cloud Gateway ingress blueprint.
- `P7.5`/`P7.6` Security Team (due 2025-10-16): Model service-to-service access matrices, author `PeerAuthentication` + `AuthorizationPolicy` manifests, and submit ADR 0006 addendum for review.
- `P7.7` DevOps Team (due 2025-10-17): Update `canary-deployment.yml` with Istio traffic shifting stages and mesh-aware smoke tests; validate CI run in non-prod environment.
- `P7.8`/`P7.10` Observability Team (due 2025-10-18): Extend Grafana dashboards with Istio telemetry, confirm SCG→Istio→service trace continuity, and document playbook updates.
- `P7.9` QA Team (due 2025-10-20): Execute integration suite covering mTLS enforcement, failure injection, and rollback paths; log findings and defects in the PHASE-7 board.

### 🧭 Phase 7 Readiness Checklist
- Capture research artifacts and IaC design notes in `docs/phases/PHASE-7.md` prior to implementation to satisfy AGENTS research practice.
- Align Istio waypoint strategy decision with open-decision tracker and update ADR 0006 once the approach is agreed.
- Ensure CI environments include mesh components before running smoke tests; record adjustments in `docs/runbooks/service-mesh.md`.

---
_Last updated: 2025-10-11_
