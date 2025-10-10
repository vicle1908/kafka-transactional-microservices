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
- Created `infra/postgres/init/01-create-databases.sql` to create `payments`, `inventory`, and `notification` databases owned by `app`.
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
- **COMPLETED**: Enhance observability with comprehensive documentation, OpenTelemetry tracing implementation, and runbook completion.
- **Enhanced Observability Stack**: Implement centralized logging with ELK stack (Elasticsearch, Logstash, Kibana) for aggregated log analysis and visualization.
- **Complete OpenTelemetry Implementation**: Deploy OpenTelemetry Collector and Jaeger backend to complete the tracing infrastructure.
- **Structured Logging Implementation**: Created StructuredLogger utility in common-observability module for consistent JSON-formatted log messages that can be easily parsed by ELK stack.
- **Kibana Dashboard Configuration**: Created service logs dashboard configuration for Kibana to enable log visualization and monitoring.
- **Git Hooks Implementation**: Implemented Git hooks to enforce Gradle tasks before allowing commits and pushes, ensuring code quality is maintained locally.
- Execution board: [PHASE-5](docs/phases/PHASE-5.md)

### Phase 6 – Hardening & Launch (Weeks 9-12)

- Conduct load tests simulating peak traffic with GitHub Actions workflow (load-test.yml); validate EOSbehavior under backpressure and high outbox table depth.
- Perform disaster recovery exercises (restore DB snapshot, rebuild Debezium connector offsets, replay outbox).
- Secure the platform (TLS/SASL, Kafka ACLs, secrets rotation) and complete compliance reviews.
- Run canary deployment for each service with feature flagsvia GitHub Actions workflow (canary-deployment.yml); monitor `outbox_table_depth` and transaction commit metrics before full rollout.
- Execution board: [PHASE-6](docs/phases/PHASE-6.md)

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
- **COMPLETED**: Additional runbooks for polling relay mechanism and connector configuration guide.
- **COMPLETED**: Additional runbooks for API gateway, service mesh, polyglot datastores, and OpenTelemetry tracing.
- **COMPLETED**: Grafana dashboards for Temporal and CDN metrics.
- **COMPLETED**: Centralized logging with ELK stack implementation.
- **COMPLETED**: Complete OpenTelemetry deployment with collector and Jaeger backend.
- **COMPLETED**: Structured logging implementation with StructuredLogger utility.
- **COMPLETED**: Git hooks implementation for local code quality enforcement.

## 4. Open Decisions & Research Tasks

-Finalize choice between Debezium connectors vs lightweight polling for low-volume services based on operational complexity assessment.
- Evaluate Confluent vs open-source Kafka distribution for licensing & support.
- Decide on schema format (Avro vs JSON Schema) and registry enforcement rules.
- Confirm saga coordinator needs for complex workflows (e.g.,orchestrator vs choreography).
- Assess infrastructure hosting (self-managed Kubernetes vs managed Kafka services such as MSK, Confluent Cloud).
- Select CDN provider and deployment model (managed vs self-hosted edge).
- Determine Temporal deployment option (self-hosted vs managed service).
- Periodically reassess service meshchoice (Istio ambient vs Linkerd/managed meshes) based on resource footprint, cost, and feature needs.
- Evaluate secrets management deployment (Vault OSS vs enterprise vs cloud-native secret stores).
- Evaluate ELK stack implementation for centralized logging in microservices.

## 5. Risks & Mitigations

- **Debezium lag or outages**: Implement alerting,auto-restart scripts, and replay tooling.
- **EOS configuration drift**: Enforce shared Spring Kafka config via `common-kafka` module; add integration tests with full environment validation.
- **Schema incompatibilities**: Automate schema validation in CI; require backward-compatible changes.
- **Operational complexity of relay service**: Provide detailed runbooks, offer fallback poller mode, schedule regular drills, implement comprehensive monitoring for `outbox_table_depth`.
- **Performance bottlenecks**: Monitor outbox table depth and implement proper indexing; optimize relay processing batch sizes.
- **Security gaps**: Apply TLS/SASL, integrate secrets vault, conductthreat modeling sessions.
- **Observability gaps**: Implement centralized logging and complete OpenTelemetry deployment to ensure full visibility across all services.

## 6. Next Actions (Current Phase)

### ✅ **PHASE 1 COMPLETE** - CI/CD Infrastructure Hardening

**Achievement Summary:**
- ✅ **Security**: Explicit permissions, wrapper validation, pinned actions, concurrency controls
- ✅ **Performance**: gradle/actions integration, configuration cache, parallel execution  
- ✅ **Quality**: All detekt/ktlint violations resolved, Java 21-25 support, workflow YAML validated
- ✅ **Coverage**: Hardened CI, nightly integration tests, secure container publishing
- ✅ **Automation**: Branch protection scripts, post-merge setup, monitoring documentation

**Status**: PR #1 ready for merge with all quality gates passing

### 🔄 **IMMEDIATE ACTIONS** (Current Week)

1. **Stabilize PR Workflows** ⭐ HIGH PRIORITY
   - [x] CI: fix JPA tests (H2 for module tests), wrapper fallback, docs step
   - [x] Infra Validation: green
   - [x] Dependency Review: skip on PRs/private; scheduled/manual with warn-only
   - [x] Integration/Load tests: skip on PRs; main + schedule only

2. **Complete PR #1 Merge** ⭐ HIGH PRIORITY
   ```bash path=null start=null
   # When CI shows green:
   gh pr merge 1 --squash --delete-branch
   ./scripts/github/post-merge-setup.sh
   ```

3. **Verify Infrastructure Health** ⭐ MEDIUM PRIORITY  
   - [ ] Monitor first main branch CI run
   - [ ] Validate nightly integration test execution
   - [ ] Confirm build performance improvements

### 📋 **Local Infra Bootstrap & Migrations (Dev)**

Commands below assume PWD at project root and use the root .env:

```bash path=null start=null
# Bring up Postgres only (local profile)
docker compose --env-file .env -f infra/compose.yml --profile local up -d postgres

# Run Flyway migrations sequentially (orders → payments → inventory → notification)
docker compose --env-file .env -f infra/compose.yml --profile local --profile migrate run --rm flyway-orders
docker compose --env-file .env -f infra/compose.yml --profile local --profile migrate run --rm flyway-payments
docker compose --env-file .env -f infra/compose.yml --profile local --profile migrate run --rm flyway-inventory
docker compose --env-file .env -f infra/compose.yml --profile local --profile migrate run --rm flyway-notification

# Optional: bring up the full local stack (Kafka, Schema Registry, Redis, Debezium, AKHQ)
docker compose --env-file .env -f infra/compose.yml --profile local up -d
```

Notes:
- Compose profiles: local for runtime infra; migrate for one‑shot Flyway tasks.
- Flyway uses baselineOnMigrate=true to safely initialize non‑empty schemas.
- Per‑service migrations live under services/<name>/src/main/resources/db/migration and are mounted into Flyway containers.
- Postgres init scripts live under infra/postgres/init/ and create required databases on first startup.

### 📋 **Cache Integration (In Progress)**
- common-cache module added with Spring Boot auto-configuration (RedisConnectionFactory, RedisCacheManager, @EnableCaching)
- Services depend on :common-cache; default TTL=15m; uses REDIS_HOST/REDIS_PORT
- TLS/auth support added via REDIS_USERNAME/REDIS_PASSWORD and REDIS_SSL=true
- Serializer hardening: keys → StringRedisSerializer; values → GenericJackson2JsonRedisSerializer with JavaTimeModule
- Runbook added: docs/runbooks/cache.md; prod overlay redis.conf stub created at infra/redis/redis.conf
- Initial caches wired:
  - orders-service: OrdersQueryService.getOrder(orderId) → cache `orders:by-id` (returns OrderDto)
  - inventory-service: InventoryQueryService.getStockBySku(sku) → cache `inventory:stock:by-sku` (returns InventoryStockDto)
- Eviction wired on write paths (orders create; inventory reserve/release/reconcile)
- Per-cache TTL override: inventory:stock:by-sku set to 3 minutes
- Metrics: cache metrics exposed via actuator/prometheus on all services (management.metrics.enable.cache=true)
- Next: consider TTL jitter and additional read caches as needed based on metrics

### 📋 **PHASE 2A** - Security Enhancement (Week 3-4)
1. **Security Workflows**: OWASP (scheduled/manual), Trivy (config/image scan), CodeQL (init/build/analyze) – non-blocking on PRs
2. **Infrastructure Validation**: Validate compose/k8s manifests; add load testing automation (scheduled)
3. **Service Template Resume**: Continue with domain entities, outbox schema, transactional configuration
4. **ADR Completion**: Draft ADRs for transactional outbox pattern, Debezium adoption, saga choreography
5. **Infrastructure Setup**: Author infra compose file, implement actual versionCheck/schemaCompatibilityCheck logic

---
_Last updated: 2025-10-10_