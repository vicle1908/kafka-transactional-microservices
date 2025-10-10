# Kafka Transactional Microservices – Agent Guide

## Scope & Outcomes
- Build Spring Boot microservices that coordinate business state changes with Kafka events without dual writes.
- Guarantee at-least-once delivery everywhere and exactly-once semantics for read→process→write flows that update a database and publish to Kafka.
  -Keep@IMPLEMENTATION_PLAN.md current; treat it as the source for milestones, deliverables, and sequencing.

## Architectural Guardrails
- Apply the transactional outbox pattern so domain data and outbound events commit in the same RDBMS transaction; use Debezium Outbox SMT or a polling relay as thepublisher.
- Wire business transactions through Spring for Apache Kafka using `KafkaTransactionManager` so DB work and Kafka offset commits succeed or roll back together.
- Enable idempotent Kafka producers (`enable.idempotence=true`, `acks=all`) and configure consumers with `isolation.level=read_committed`; persist idempotencykeys to shield downstream side effects.
- Keep saga choreography lightweight: prefer domain events plus compensating actions over distributed 2PC; reserve orchestration for cross-domain long-running transitions.

## Architecture Blueprint
- Apply Hexagonal (Ports & Adapters) layering inside every service: domain/core modules contain entities, valueobjects, and domain services; application modules expose use-cases and transaction boundaries; adapters implement inbound transports (REST, gRPC, Kafka listeners) and outbound integrations (repositories, downstream APIs). This isolates business logic from frameworks and eases testing.
- Treat each microservice as a bounded context following Domain-Driven Design.Alignaggregates and repositories to business capabilities, avoid cross-context entity reuse, and surface integration exclusively through stable APIs or domain events.
- Package code by feature slice (`orders.application`, `orders.adapter.outbound.events`) rather than by technical tier to keep related classes co-located while respecting hexagonal layers. Enforce module boundaries withGradle conventions and restricted visibility.
- Centralize edge concerns in an API gateway (Spring Cloud Gateway or equivalent) for routing, authN/Z, rate limiting, schema validation, and trace propagation. Downstream services stay focused on domain logic and publish events for cross-service workflows.
- Reserve synchronous service-to-service calls for boundedcontextsthat truly need request/response semantics; when required, standardize on gRPC with Protobuf IDLs so contracts remain type-safe while Avro continues to back Kafka event streams. Document mappings between Protobuf DTOs and Avro domain events to avoid drift.
- Keep infrastructure concerns (messaging clients, persistence configs,observability) in dedicated adapter modules to make technology swaps (e.g., Kafka → Pulsar) incremental without rewriting the domain core.

## Service Connectivity Strategy
- North-south traffic: Spring Cloud Gateway remains the dedicated edge gateway, handling authentication, rate limiting, request/response shaping, and contract enforcement before requests reach internalmeshes.
- East-west traffic: Deploy Istio 1.24+ in ambient mode for sidecar-less service mesh capabilities (mTLS, traffic policy, zero-trust, observability) while retaining compatibility with Kubernetes Gateway API. Capture mesh configuration under `infra/istio/` and automate via Helm/Terraform.
- Integration approach: Gateway routes terminate at mesh ingress gateways; Istio manages service-to-service policy, retries, and telemetry. Use Ambient waypoints for L7 policies on critical namespaces.
- Multi-cluster readiness: Track Istio ambient multicluster progress (1.27+ features) and prototype failoverscenariosbefore production adoption.

## Platform Baseline
- Languages & runtimes: Kotlin 2.2.20 as the primary implementation language running on Java 25 (latest GA) with Java 23 and Java 21 retained as fallbacks. Enable preview JVM features only behind build profiles and documentpromotiondecisions. Keep Kotlin `jvmTarget` at 21 until the compiler adds bytecode support beyond Java 24.
- Frameworks: Spring Boot 3.5.6 with Spring Framework 6.2.11 and Spring for Apache Kafka 3.3.10 (EOS v2);monitor3.5.x patch releases and prepare for Spring Boot 4.0 milestones as needed.
- Build & tooling: Gradle 9.1.0 (Kotlin DSL), Testcontainers 1.20+, Docker Engine 27+, and OpenTelemetry SDK 1.44+bakedintothe shared parent project.
- Serialization & JSON: Keep Spring Boot 3.5.x defaults and pin `com.fasterxml.jackson` artifacts to 2.20.0 for security fixes; use Kotlinx Serialization (`Json.Default`) to build structured payload fragments before embedding them in Avro event envelopes.
- Messaging: Apache Kafka 4.1.0 (KRaft-only metadata) or Confluent Platform 8.x with Schema Registry; set `transaction.state.log.replication.factor >= 3`, `transaction.max.timeout.ms <= 900000` (15m), and `min.insync.replicas >= 2`.
- Data: PostgreSQL 15+ (logical replication enabled) or MySQL 8.0.32+; define `outbox` tables per bounded context with JSON payload + metadata fields (`event_id`, `aggregate_type`, `aggregate_id`,`occurred_at`). Note alternative datastores (document DBs, key-value) in service ADRs and justify integration approach (CDC bridge, change-projection service, etc.).
- CDC: Debezium 3.3.0.Final connectors with Outbox Event Router SMT are the default outboxpublisher;maintain connector manifests under `infra/debezium/` and adhere to platform backup/runbook requirements. A lightweight polling relay may be enabled only when CDC access is unavailable—record the exception in the service ADR and implement the retry/backoff settings defined in @IMPLEMENTATION_PLAN.md.
- Serialization: AvroorJSONSchemaenforced through the registry; version message contracts via Gradle module `common-events`. Avoid direct Jackson dependencies in shared modules—wire actual serializers (Spring Kafka JSON, Avro, or Kotlinx) per service adapter when needed.

## Version & Compatibility Policy
- Default to the latest stable GA releases: Java25,Spring Boot 3.5.x, Spring for Apache Kafka 3.3.x, Kafka 4.1.x, Debezium 3.3.x, Gradle 9.1.x. Record the prior LTS versions in `docs/version-matrix.md` as fallbacks with downgrade guidance.
- Maintain`docs/version-matrix.md` listing each service’s current, candidate, and fallback versions (JDK, Spring Boot, Kafka client, Debezium connector, Testcontainers) plus compatibility notes.
- Run a quarterly dependency review (see Research Backlog) to validate new maintenance drops; require smoke tests,upgradeplaybooks,and rollback plans before updating production baselines.
- Use CI checks (Gradle task `versionCheck`) to flag mismatched runtime versions across services; merge is blocked until the matrix is updated or the mismatch is resolved.

## Dev Workflow & Commands
- Bootstrap infra with `docker compose up -d kafka postgresschema-registrydebezium` (compose file will live under `infra/compose.yml`).
- Run all service tests with `./gradlew clean test` and integration tests with `./gradlew :service-* :integration-test` once modules exist.
- Start a sample service locally via `./gradlew :orders-service:bootRun` (ensure `.env` contains broker/bootstrap endpoints and DB creds).
- Use `./gradlew flywayMigrate` to apply schema migrations before running services.
- Lint/format with `./gradlew spotlessApply` (add plugin in the build once codebase is scaffolded).
- Wheneditingorinspecting code via JetBrains MCP server, open the target file with `open_file_in_editor` before running `get_file_problems` so IntelliJ indexes the file, then review errors/warnings ahead of Gradle tasks.
- Export Avro schemas with `./gradlew exportAvroSchemas` and publish via`scripts/schema-publish.sh` before enabling Debezium connectors; registry compatibility is enforced in CI.
- Service modules live under `services/<name>` (e.g., `orders-service`) and follow the hexagonal template documented in `docs/dev/service-template.md`; depend on shared modules for events, Kafka, and persistence.

##MCP Search Practice
- Triage query type first: use `mcp-router__brave_web_search` for broad web research, switching to `mcp-router__brave_news_search` when freshness (≤7 days) matters and `mcp-router__brave_image_search` for visual assets.
- For deep-dive investigations build amap→extract pipeline: `mcp-router__tavily_map` to enumerate relevant docs, then `mcp-router__tavily_extract` (or `mcp-router__tavily_search` with `search_depth='advanced'`) for full-text pulls; request `include_raw_content` when evaluating technicalspecs.
- When questions target repository docs or architecture notes, reach for DeepWiki (GitHub doc crawler) before general search; follow with `mcp-router__brave_web_search` only if the repo lacks internal docs.
- For library and framework-specific research, use `mcp-router__resolve-library-id` to findthe correct Context7-compatible library ID, then `mcp-router__get-library-docs` to retrieve up-to-date documentation.
- For real-world code examples and implementation patterns, leverage `mcp-router__searchGitHub` to find relevant code from over a million public repositories.
- Prefer Brave over raw Google for low-latency fact checks; fall back to Tavily advanced search or `mcp-router__web_search_exa` when Brave results are thin or contradictory.
- For programming-related questions, leverage `mcp-router__get_code_context_exa` to find relevant context for APIs, libraries, and SDKs with the highest qualityand freshest context.
- When validating internal knowledge, use `mcp-router__search_in_files_by_text` (grep-mcp) or `mcp-router__start_search` with `searchType='content'` to cross-check local docs before escalating to web tools.
- Leverage Medium-focused research with `mcp-router__search_medium_topic` for comprehensive topic-based research, `mcp-router__search_by_author` to find insights from domain experts, and `mcp-router__research_compilation` for multi-topic synthesis with citations.
- Always capture tool outputs in the working note, link to @IMPLEMENTATION_PLAN.md action items, and record gaps in the Research Backlog when sources are inconclusive.
- For complex decisions, use `mcp-router__consensus` (consulting Gemini, OpenAI, Grok-4 via Zen MCP) to gather multiple AI perspectives, then apply `mcp-router__thinkdeep` when deeper reasoning or resolution is required.
- When researching, combine available search MCP tools (`mcp-router__brave_web_search`, `mcp-router__tavily_search`, `mcp-router__web_search_exa`, `mcp-router__searchGitHub`, `mcp-router__search_medium_topic`) to gather evidence before consulting Zen MCP (`consensus`, `thinkdeep`) for multi-model evaluation.

## Knowledge Memory Practice
- Persist key decisions, architectural patterns, runbook updates, and milestonecompletions with `mcp-router__add-memory` using succinct, action-oriented phrasing; tag entries with the relateddocpath (`@AGENTS`, `@IMPLEMENTATION_PLAN`, etc.).
- Before starting research or planning work, run `mcp-router__search-memories` with focus keywords (e.g., `Kafka outbox`, `Debezium`) to surface prior conclusions and avoid duplicate effort.
- Summariescaptured in memory must also be reflected in the canonical docs (`@AGENTS.md`, `IMPLEMENTATION_PLAN.md`) so stored memories stay consistent with living documentation.
- When decisions change, append new memoriesreferencing the superseded choice and update affected docs immediately; never delete prior memories—record reversals explicitly.
- Review memory entriesduring weekly syncs to spot outdated assumptions and schedule doc or implementation updates as needed.

## Code Style & Quality
- Adopt Spring’s 2025 Java code style (Google-derived):UTF-8, LF endings, tab indentation, 120-character line target, and no trailing whitespace. Configure IDEstohonor the repo `.editorconfig`, and use `./gradlew ktlintFormat` to auto-format Kotlin sources; CI enforces `ktlintCheck` and `detekt` oneverychange.
- Use constructor injection for Spring-managed components, keep controllers/services package-private unless cross-module visibility is required, andleverage Java records or Lombok-free value classes for immutability at the edges.
- Separate domain, application, and adapter DTOs; map via MapStruct or dedicated translators to avoid leakingpersistenceor transport annotations into core logic.
- Layer tests: domain/application tests run without Spring (JUnit 5 + AssertJ); adaptertests rely on Testcontainers for Kafka/Postgres, and consumer/producer contract tests validate event schemas in `common-events`.
- Enforce static analysis and QA tooling: Detekt,ktlint, dependency-vulnerability scanning, and Jacoco thresholds aligned with critical paths (domain/application ≥90%, adapters ≥75%); plan SpotBugs + Error Prone adoption during service hardening.
- Caching strategy: adopt Redis (cache-aside/lazy population) for high-read workloads;standardize TTLs, invalidation hooks, and security hardening per Redis guidance.
- Content delivery: leverage CDN/edge caching(e.g., AWS CloudFront, Cloudflare) for static assets and API caching headers; integrate cache-busting into CI pipelines.

## DevOps & Delivery Practices
- Infrastructure-as-code (Terraform/Helm) governs Kubernetes, Istio, and gateway deployments; peer-review and lint all IaC changesinCI.
- CI/CD pipelines (GitHub Actions/GitLab) automate lint, tests, security scans, container builds, schema compatibility checks, and progressive delivery with canaries.
- Gradlebuilds must run with configuration cache and build cache enabled (`org.gradle.configuration-cache=true`, `org.gradle.caching=true`);CIinvokes `./gradlew --configuration-cache` and developers should prefer the same for local workflows.
- Run `./gradlew schemaCompatibilityCheck` to validate Avro schemas before publishing; CIexecutes the task alongside `check`.
- Observability-first: enforce OpenTelemetry instrumentation, centralize logs/metrics, and maintaindashboards/alerts for latency, errors, saturation, and business SLIs.
- Resilience engineering: run regular chaos drills (broker restarts, mesh failures, cache outages) andrecord findings in runbooks.

## Data Consistency Workflow
- Within each command handler, persist domain aggregates and append an outboxrowinside one transaction; mark unsent events with `status='NEW'`.
- Debezium connectors stream `NEW` outbox rows to Kafka topics; configure Outbox SMT to maptable columns to payload + headers (`eventId`, `schemaVersion`, tracing context). Polling relays are reserved for CDC-restrictedenvironments and must follow the shared retry/backoff configuration (`maxAttempts=5`, exponential backoff starting at 500 ms).
- Consumers process messages idempotently: check a`processed_events` table keyed by `event_id` before invoking side effects; commit offsets only after success.
- Application services validate commands prior to persistence (non-blank customer identifiers, non-empty item collections) and reuse a single captured `Instant` for domain writes, Avro event timestamps, and outbox records to ensure envelope consistency.
- Mirror the orders pattern in `payments-service`: compute payment payloads via Kotlinx Serialization, emit `PaymentCompletedEvent` Avro records, and persist payment aggregates as `COMPLETED` alongside outbox entries within one transaction; Kafka consumer wiring follows once topic contracts are defined.
- Maintain a shared `processed_events` ledger per service (JPA entity + Flyway migration) to short-circuit duplicate payloads inside Kafka listeners; wrap listener handlers in Spring transactions backed by `KafkaTransactionManager` so ledger writes, payment persistence, and offset commits share a unit of work.
- Inventory service consumes `PaymentCompletedEvent` messages, resolves stock reservations via `InventoryService.reserve`, and publishes `InventoryReservedEvent` records through the transactional outbox to notify downstream consumers.
- For long-running sagas, store state transitions in a dedicated table and emit compensating events on failure paths.

## Saga Implementation Patterns
- Represent each saga as a state machine persisted in `sagas` tables(`saga_id`, `type`, `state`, `data`, `updated_at`); transitions are triggered by inbound events or timeouts. Use optimistic locking to prevent concurrent updates.
- Reuse `common-sagas` shared module for saga persistence: `SagaStateService` manages start/transition/complete/fail flows with optimistic locking and correlation-id uniqueness.
- Adopt Temporal (self-hosted or cloud) as the orchestrator for complex, multi-domain sagas to centralize state, retries,and compensation logic; keep event-choreographed sagas for simple two-service flows.
- Emit compensating commands/events from the application layeror Temporal activities when a step fails; include correlation identifiers and reason codes so downstream services can reconcile partial changes.
- Provide idempotent handlers by combining processed-event ledgers withbusiness keys(e.g., `order_id`); return early if the saga step has already completed.
- Document saga flows with sequence diagrams in`docs/sagas/` and include contract tests that replay happy-path, compensating, and timeout scenarios.

## Schema Evolution Policy
- Enforce backward compatibility for message schemas (Avro`BACKWARD` mode, JSON Schema additive changes only). Breaking changes require a new topic or schema version with dual-publish strategy.
  -Automate schema validation in CI using `./gradlew schemaCompatibilityCheck`; merges fail if compatibility rules are violated.
- Record schema change intents in `docs/schemas/CHANGELOG.md`with effective dates, owner, and impacted services. Notify consumer teams via shared channels before rollout.
- For database schemas, apply Flywaymigrations with reversible scripts where feasible and maintain downgrade instructions in the migration description.

## Reliability & Operations
- Configure retry/DLQ topics per domain (`<topic>.retry`,`<topic>.dlq`) with exponential backoff handled in Spring Retry templates or Kafka Streams topologies.
- Monitor transaction aborts, outboxlag, Debezium lag, and consumer group lag via Micrometer + Prometheus; expose dashboards in Grafana.
- Propagate distributed tracing context via W3C Trace Contextheaders; integrate OpenTelemetry SDK in each service.
- Document standard incident runbooks under `docs/runbooks/` (broker outage,Debezium connector restart, replay outbox).

## Operational Automation & Gateway Runbooks
- Store API gateway playbooks under `docs/runbooks/api-gateway.md`, covering route deployment,canary rules, rate-limit tuning, auth provider rotation, and rollback steps. Integrate static contract checks to ensure gateway transformers match downstreamschemas.
- Define CI/CD pipelines (GitHub Actions templates under `.github/workflows/`) that run ktlint, detekt, schema compatibility checks, unit and integration tests,pluscontainer image builds with vulnerability scanning.
- Maintain Debezium connector runbook (`docs/runbooks/debezium.md`) covering schema export(`./gradlew exportAvroSchemas`), registry publication, and replay tooling (`scripts/outbox-replay.sh`).
- Automate Debezium connector lifecycle using Infrastructure-as-Code (Terraform/Helm) alongside smoke tests that validate lag and schema mappings after each deployment.
- For services using non-relational stores or polyglot runtimes, include adapter-specific health checks and replication monitoring in their runbooks; reference the shared version matrix to confirm driver compatibility.

## Security & Compliance- Enforce TLS and SASL for Kafka brokers; manage ACLs so services only access their topics.
- Store credentials with Vault orAWSSecrets Manager; never commit secrets.
- Classify events that carry PII; mask or tokenize sensitive fields before publishing.
- Kubernetes secrets: prefer Vault Agent Injector or SecretsOperatorto deliver short-lived credentials; enforce RBAC and audit logging around secret access.

## Immediate Follow-up Actions
- Author `docs/version-matrix.md` and implement the Gradle `versionCheck` task that validates runtime dependencies before merge.
- Build an end-to-end saga pilot (Order → Payment → Inventory) and commitaccompanying documentation under `docs/sagas/` with sample code and contract tests.
- Add the `schemaCompatibilityCheck` Gradle taskand wire itinto CI pipelines alongside ktlint/detekt and the future SpotBugs/ErrorProne gates.
- Finish API gateway, Debezium connector, and polyglotdatastore runbooks referenced in Operational Automation; link them from `docs/runbooks/`.

## Documentation Hygiene
- Keep @IMPLEMENTATION_PLAN.md synchronizedwith roadmap changes; link additional subsystem guides using `@docs/<name>.md`.
- Record schema evolution decisions in `docs/schemas/CHANGELOG.md`; require backward-compatiblechanges unless otherwise approved.
- Maintain `docs/architecture/service-catalog.md`, `docs/notes/phase-0-*.md`, and ADRs in `docs/adrs/` as the source of truth for discovery outcomes.
- Ensure `docs/dev/getting-started.md`stays current with tooling andworkflow changes.
- Keep `docs/dev/build-system.md` updated when adding new modules, composite builds, or build logic conventions.

## Research Backlog& References
- Review Debezium high-availability sample for relay failover patterns (see GitHub repo linked below).
- Follow Spring Kafka EOSguidance to avoid regression in transactional containers.
- Track Confluent Platform release notes for broker-side transaction updates and licensing changes.
- Schedule a quarterly version review using`mcp-router__brave_web_search` (with `freshness='pm'`) and `mcp-router__tavily_extract` to capturechangelog highlights for Spring Boot, Kafka, Debezium, and Gradle.
- Key references:
    - Spring Kafka exactly-once & transactions documentation.
- Spring CloudStream blog on EOS patterns with JPA transactions.
- Confluent Platform 8.0 release notes (KRaft-first transactions & licensingupdates).
- Debezium 3.3 release notes (EOS support and connector updates).
- Debezium outbox pattern implementations (anarefin/high-availability-debezium, YunusEmreNalbant/transactional-outbox-pattern-with-debezium, chfern/debezium-outbox-pgkafka).
