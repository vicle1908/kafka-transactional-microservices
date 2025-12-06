---
trigger: always_on
---

# Kafka Transactional Microservices – Agent Guide

## Scope & Outcomes

- Build Spring Boot microservices that coordinate business state changes with Kafka events without dual writes.
- Guarantee at-least-once delivery everywhere and exactly-once semantics for read→process→write flows that update a database and publish to Kafka.
- Keep `@IMPLEMENTATION_PLAN.md` current; treat it as the source for milestones, deliverables, and sequencing.

## Quick Reference

- **Version Matrix**: [`docs/version-matrix.md`](docs/version-matrix.md) - Current/candidate/fallback versions
- **Observability**: [`docs/observability.md`](docs/observability.md) - Comprehensive observability guide
- **Runbooks**: [`docs/runbooks/`](docs/runbooks/) - Operational procedures
- **ADRs**: [`docs/adrs/`](docs/adrs/) - Architecture decision records
- **Getting Started**: [`docs/dev/getting-started.md`](docs/dev/getting-started.md) - Developer setup guide
- **Service Template**: [`docs/dev/service-template.md`](docs/dev/service-template.md) - Service scaffolding guide

## Architectural Guardrails

- Apply the transactional outbox pattern so domain data and outbound events commit in the same RDBMS transaction; use Debezium Outbox SMT or a polling relay as the publisher. See [`docs/adrs/0001-transactional-outbox.md`](docs/adrs/0001-transactional-outbox.md) and [`docs/runbooks/debezium-connector.md`](docs/runbooks/debezium-connector.md).
- Wire business transactions through Spring for Apache Kafka using `KafkaTransactionManager` so DB work and Kafka offset commits succeed or roll back together.
- Enable idempotent Kafka producers (`enable.idempotence=true`, `acks=all`) and configure consumers with `isolation.level=read_committed`; persist idempotency keys to shield downstream side effects.
- Configure Kafka consumers with proper transaction management by setting `containerProperties.kafkaAwareTransactionManager` instead of the deprecated `transactionManager` property. See [`docs/dev/kafka-transaction-config.md`](docs/dev/kafka-transaction-config.md).
- Keep saga choreography lightweight: prefer domain events plus compensating actions over distributed 2PC; **reserve orchestration for cross-domain long-running transitions** using Temporal workflow orchestration implemented in `common-temporal`. See [`docs/sagas/`](docs/sagas/) and [`docs/runbooks/temporal.md`](docs/runbooks/temporal.md).

## Library & Framework Practices

- Before introducing new libraries or implementing features, research existing usage patterns in the codebase first.
- Use documentation tools (Context7, DeepWiki, GitHub search) to understand proper library usage before implementation.
- Follow existing patterns in shared modules for consistency across services.
- Verify that new library dependencies align with the established tech stack and version matrix.
- Prefer Spring Boot auto-configuration patterns over custom configurations where available.
- Maintain dependency compatibility across all modules as documented in [`docs/version-matrix.md`](docs/version-matrix.md).

## Version Management Practices

- Regularly check for newer versions of key dependencies using authoritative sources:
  - MavenCentral Repository (<https://mvnrepository.com/>) for Java/Kotlin libraries
  - Gradle Plugin Portal (<https://plugins.gradle.org/>) for Gradle plugins
  - Official project release pages and GitHub repositories
- When evaluating version upgrades, consider compatibility, security fixes, performance improvements, migration effort, and LTS vs. latest tradeoffs.
- Document version decisions in [`docs/version-matrix.md`](docs/version-matrix.md) with rationale for selections.
- Run comprehensive tests (unit, integration, contract) after version upgrades.
- Update the Gradle version catalog (`gradle/libs.versions.toml`) with new versions following semantic versioning conventions.
- Enforce the use of the Gradle version catalog for all plugins and dependencies in build files to ensure consistent version management across all modules.

## Architecture Blueprint

- Apply Hexagonal (Ports & Adapters) layering inside every service: domain/core modules contain entities, value objects, and domain services; application modules expose use-cases and transaction boundaries; adapters implement inbound transports (REST, gRPC, Kafka listeners) and outbound integrations (repositories, downstream APIs). This isolates business logic from frameworks and eases testing.
- Provide shared gRPC and message contracts via the `common-proto` module; services should import generated stubs instead of defining ad-hoc protos.
- Treat each microservice as a bounded context following Domain-Driven Design. Align aggregates and repositories to business capabilities, avoid cross-context entity reuse, and surface integration exclusively through stable APIs or domain events.
- Package code by feature slice (`orders.application`, `orders.adapter.outbound.events`) rather than by technical tier to keep related classes co-located while respecting hexagonal layers. Enforce module boundaries with Gradle conventions and restricted visibility.
- Centralize edge concerns in an API gateway (Spring Cloud Gateway or equivalent) for routing, authN/Z, rate limiting, schema validation, and trace propagation. Downstream services stay focused on domain logic and publish events for cross-service workflows. See [`docs/runbooks/api-gateway.md`](docs/runbooks/api-gateway.md).
- Reserve synchronous service-to-service calls for bounded contexts that truly need request/response semantics; when required, standardize on gRPC with Protobuf IDLs so contracts remain type-safe while Avro continues to back Kafka event streams. Document mappings between Protobuf DTOs and Avro domain events to avoid drift.
- Inventory service exposes the `inventory.v1.StockReconciliationService` gRPC endpoint (configurable via `inventory.grpc.port`) for bulk stock adjustments; enable/disable access through gateway routing and ensure synchronous clients honor saga invariants.
- Keep infrastructure concerns (messaging clients, persistence configs, observability) in dedicated adapter modules to make technology swaps (e.g., Kafka → Pulsar) incremental without rewriting the domain core.

## Service Connectivity Strategy

- North-south traffic: Spring Cloud Gateway remains the dedicated edge gateway, handling authentication, rate limiting, request/response shaping, and contract enforcement before requests reach internal meshes.
- East-west traffic: Deploy Istio 1.24+ in ambient mode for sidecar-less service mesh capabilities (mTLS, traffic policy, zero-trust, observability) while retaining compatibility with Kubernetes Gateway API. Capture mesh configuration under `infra/istio/` and automate via Helm/Terraform. See [`docs/runbooks/service-mesh.md`](docs/runbooks/service-mesh.md).
- Integration approach: Gateway routes terminate at mesh ingress gateways; Istio manages service-to-service policy, retries, and telemetry. Use Ambient waypoints for L7 policies on critical namespaces.
- Multi-cluster readiness: Track Istio ambient multicluster progress (1.27+ features) and prototype failover scenarios before production adoption.

## Platform Baseline

- Languages & runtimes: Kotlin 2.2.20 as the primary implementation language running on Java 25 (latest GA) with Java 23 and Java 21 retained as fallbacks. Enable preview JVM features only behind build profiles and document promotion decisions. Keep Kotlin `jvmTarget` at 21 until the compiler adds bytecode support beyond Java 24.
- Frameworks: Spring Boot 3.5.7 with Spring Framework 6.2.11 and Spring for Apache Kafka 3.3.10 (EOS v2); monitor 3.5.x patch releases and prepare for Spring Boot 4.0 milestones as needed.
- Build & tooling: Gradle 9.1.0 (Kotlin DSL), Testcontainers 1.20+, Docker Engine 27+, and OpenTelemetry SDK 1.44+ baked into the shared parent project.
- Serialization & JSON: Keep Spring Boot 3.5.x defaults and pin `com.fasterxml.jackson` artifacts to 2.20.0 for security fixes; use Kotlinx Serialization (`Json.Default`) to build structured payload fragments before embedding them in Avro event envelopes.
- Messaging: Apache Kafka 4.1.0 with pure KRaft metadata mode (no ZooKeeper dependency) or Confluent Platform 8.x with Schema Registry; set `transaction.state.log.replication.factor >= 3`, `transaction.max.timeout.ms <= 900000` (15m), and `min.insync.replicas >= 2`.
- Data: PostgreSQL 18 (logical replication enabled) or MySQL 8.0.32+; define `outbox` tables per bounded context with JSON payload + metadata fields (`event_id`, `aggregate_type`, `aggregate_id`, `occurred_at`). See [`docs/runbooks/polyglot-datastore.md`](docs/runbooks/polyglot-datastore.md).
- CDC: Debezium 3.3.0.Final connectors with Outbox Event Router SMT are the default outbox publisher; maintain connector manifests under `infra/debezium/` and adhere to platform backup/runbook requirements. A lightweight polling relay may be enabled only when CDC access is unavailable—record the exception in the service ADR and implement the retry/backoff settings defined in `@IMPLEMENTATION_PLAN.md`. See [`docs/runbooks/debezium-connector.md`](docs/runbooks/debezium-connector.md) and [`docs/runbooks/polling-relay.md`](docs/runbooks/polling-relay.md).
- Serialization: Avro or JSON Schema enforced through the registry; version message contracts via Gradle module `common-events`. Avoid direct Jackson dependencies in shared modules—wire actual serializers (Spring Kafka JSON, Avro, or Kotlinx) per service adapter when needed.

## Version & Compatibility Policy

- Default to the latest stable GA releases: Java 25, Spring Boot 3.5.7, Spring for Apache Kafka 3.3.x, Kafka 4.1.x, Debezium 3.3.x, Gradle 9.1.x. Record the prior LTS versions in [`docs/version-matrix.md`](docs/version-matrix.md) as fallbacks with downgrade guidance.
- Maintain [`docs/version-matrix.md`](docs/version-matrix.md) listing each service's current, candidate, and fallback versions (JDK, Spring Boot, Kafka client, Debezium connector, Testcontainers) plus compatibility notes.
- Run a quarterly dependency review (see Research Backlog) to validate new maintenance drops; require smoke tests, upgrade playbooks, and rollback plans before updating production baselines.
- Use CI checks (Gradle task `versionCheck`) to flag mismatched runtime versions across services; merge is blocked until the matrix is updated or the mismatch is resolved.

## Docker & Container Image Management

When managing Docker infrastructure components, use the Docker CLI directly to verify latest versions rather than relying solely on web searches.

### Core Commands

```bash
# Search for official repositories
docker search --limit 5 postgres
docker search --limit 5 redis

# Query Docker Hub API directly for accurate tags
curl -s "https://registry.hub.docker.com/v2/repositories/library/postgres/tags?page_size=10" | jq -r '.results[] | "\(.name): \(.last_updated)"'

# Verify multi-architecture support
docker manifest inspect redis:latest
```

### Best Practices

1. **Prefer Official Images**: Use `docker search` to identify official repositories marked with `[OK]`
2. **Version Selection**: Use major version tags (e.g., `postgres:18`, `redis:8-alpine`), avoid `latest` in production
3. **Cross-Reference Sources**: Combine Docker CLI results with Docker Hub API calls and local cache verification
4. **Environment Management**: Maintain image versions in `infra/.env` and use environment variables in `compose.yml`

## Dev Workflow & Commands

- Bootstrap infra with env: `cp infra/.env.example infra/.env && docker compose --env-file infra/.env -f infra/compose.yml up -d`.
- Run all service tests with `./gradlew clean test` and integration tests with `./gradlew :service-* :integration-test` once modules exist.
- When you need to run shell commands, prefer the `execute_terminal_command` MCP tool so terminal interactions stay auditable and repeatable.
- Start a sample service locally via `./gradlew :orders-service:bootRun` after loading `.env` (copy from `.env.example` and use direnv or `source scripts/export-env.sh`).
- Use `./gradlew flywayMigrate` to apply schema migrations before running services.
- Lint/format with `./gradlew ktlintFormat` and `./gradlew detektAll`.
- When editing or inspecting code via JetBrains MCP server, open the target file with `open_file_in_editor` before running `get_file_problems` so IntelliJ indexes the file, then review errors/warnings ahead of Gradle tasks.
- Export Avro schemas with `./gradlew exportAvroSchemas` and publish via `scripts/schema-publish.sh` before enabling Debezium connectors; registry compatibility is enforced in CI.
- Service modules live under `services/<name>` (e.g., `orders-service`) and follow the hexagonal template documented in [`docs/dev/service-template.md`](docs/dev/service-template.md); depend on shared modules for events, Kafka, persistence, sagas, and observability.
- Launch disposable CLI subagents with `clink` when fresh context windows are needed for specific tasks. Currently, only claude, codex, and gemini are supported by clink due to a hardcoded allowlist. See [`docs/dev/clink-quick-reference.md`](docs/dev/clink-quick-reference.md) and [`docs/dev/clink-use-cases.md`](docs/dev/clink-use-cases.md).
- Always use the Gradle version catalog (`gradle/libs.versions.toml`) when adding new plugins or dependencies to build files to ensure consistent version management across all modules.

### Local Workflow & Document Validation

Before committing changes, especially to GitHub Actions workflows or documentation, run local validation tools to catch errors early:

```bash
# After editing YAML files (.yml, .yaml)
yamllint <filename>.yml
yamllint infra/ .github/

# Verify specific directories
yamllint infra/compose.yml
yamllint infra/prometheus/
yamllint infra/debezium/connectors/
yamllint .github/workflows/

# After editing Markdown files (.md)
npx markdownlint <filename>.md
npx markdownlint "**/*.md" --fix

# After editing GitHub Actions workflows
actionlint .github/workflows/*.yml

# Format Kotlin code
./gradlew ktlintFormat
```

**Tool Installation** (macOS): `brew install yamllint actionlint && npm install -g markdownlint-cli`

**YAML Linting Configuration**: The project uses `.yamllint` configuration file with:
- Line length limit: 120 characters
- Indentation: 2 spaces
- Key duplication detection enabled
- Trailing spaces and empty line rules enforced

Run `yamllint` with the project's configuration: `yamllint -c .yamllint <file-or-directory>`

## Database & CDC Setup

- Local baseline uses PostgreSQL 18 with logical replication enabled; Compose mounts init scripts under `infra/postgres/init` to create service DBs (orders, payments, inventory, notification) and `pgcrypto`.
- Schema is codified with Flyway migrations per service (`services/*/src/main/resources/db/migration`). A dedicated Compose profile `migrate` runs four one-off Flyway containers: flyway-orders, flyway-payments, flyway-inventory, flyway-notification. Apply with: `make migrate`.
- Kafka 4.1 (KRaft) is used locally. Healthcheck calls the bundled broker tool. Auto-create topics is disabled for parity with production; the internal `__consumer_offsets` topic is created explicitly.
- Debezium Connect 3.3 is the CDC default:
  - Connectors: orders, payments, inventory, notification
  - `topic.prefix` set per DB, `snapshot.mode=initial` (use `no_data` after initial sync)
  - Outbox Event Router routes to `outbox.${routedByValue}` with key=`aggregate_id`
  - `transforms.outbox.table.fields.additional.placement` excludes payload to avoid schema duplication
- Connector-side `topic.creation.default.*` is enabled for local dev so outbox topics are created when producing.
- Outbox table includes `status` column for polling relay fallback; Debezium-only environments can use lean schema.
- Standardized workflows:
  - Start local stack: `make up`
  - Run migrations: `make migrate`
  - Register/refresh connectors: `make connectors`
  - Smoke test (insert outbox row → read from Kafka): `make smoke`
  - Tear down: `make down` (or `make clean-volumes` for a full reset)

See [`docs/runbooks/debezium-connector.md`](docs/runbooks/debezium-connector.md) for detailed connector operations.

## Documentation & Context Best Practices

- **Codebase Search**: Prefer semantic codebase search (`search_code`) over traditional grep/file search. See [Codebase Indexing & Semantic Search Practices](#codebase-indexing--semantic-search-practices) for detailed workflow.
- Before adding or editing code, use documentation tools to understand existing patterns and libraries:
  - Use `search_code` to find existing implementations and patterns in the codebase (preferred method)
  - Use `get_code_context_exa` to search for relevant API/library contexts before implementing new features
  - Use `searchGitHub` to find real-world examples and implementation patterns from similar projects
  - Use `resolve-library-id` and `get-library-docs` to access up-to-date library documentation via Context7
  - Use `read_wiki_structure` and `read_wiki_contents` to explore GitHub repository documentation
- Always verify implementation approaches against existing code patterns in the repository before writing new code.
- When using external libraries, first research their correct usage patterns and configuration through documentation tools.
- Follow established patterns in shared modules (`common-*`) as templates for new implementations.

## Codebase Indexing & Semantic Search Practices

**Preferred Approach**: Use semantic codebase search (`search_code`) instead of traditional grep/file search for better context-aware results.

### Initial Setup & Verification

1. **Check Indexing Status**: Before searching, verify the codebase is indexed:
   ```bash
   # Use MCP tool: get_indexing_status
   # Verify status shows "fully indexed and ready for search"
   ```

2. **Index Codebase if Needed**: If not indexed or after major changes:
   ```bash
   # Use MCP tool: index_codebase
   # Path: Absolute path to codebase root
   # Splitter: 'ast' (syntax-aware) or 'langchain' (character-based)
   # Force: true if re-indexing existing codebase
   ```

3. **Enable File Watcher**: Ensure file watcher is active for automatic incremental updates:
   ```bash
   # Use MCP tool: get_file_watcher_status
   # Verify: Enabled = Yes, Watching Codebases includes your path
   # Enable if needed: set_file_watcher_enabled(enabled=true)
   ```

### Search Workflow

**Primary Method - Semantic Search**:
- **Always prefer `search_code`** over traditional grep/file search for:
  - Finding implementations of patterns or concepts
  - Understanding how features are implemented across the codebase
  - Discovering related code that shares semantic meaning
  - Locating code by natural language queries

- **Use `search_code` with natural language queries**:
  ```bash
  # Example: Find order creation logic
  search_code(query="How are orders created and persisted?")
  
  # Example: Find Kafka listener implementations
  search_code(query="Where are Kafka listeners processing OrderCreatedEvent?")
  
  # Example: Find Temporal activity implementations
  search_code(query="How are Temporal activities implemented in payment service?")
  ```

**When to Use Traditional Search**:
- Exact string/symbol searches (function names, class names, constants)
- File path searches
- Regex pattern matching for specific syntax
- Quick existence checks

### File Watcher Configuration

**Optimal Settings** (adjust via `update_file_watcher_config`):
- **Debounce Time**: 100-500ms (batch rapid changes)
- **Batch Size**: 50-100 changes per batch
- **Max Concurrency**: 2-5 concurrent processing operations

**Monitoring**:
- Check `get_file_watcher_status` regularly to ensure:
  - File watcher is enabled
  - Pending changes are being processed
  - No processing errors
  - Memory usage is reasonable

**Troubleshooting**:
- If index becomes stale: Force re-index with `index_codebase(force=true)`
- If file watcher stops: Check status and restart if needed
- If search results are incomplete: Verify indexing completed successfully

### Best Practices

1. **Before Major Code Changes**: Verify indexing status and file watcher health
2. **After Adding New Modules**: Check that new files are being indexed automatically
3. **For Complex Queries**: Use natural language descriptions of what you're looking for
4. **Combine Approaches**: Use `search_code` for discovery, then grep for exact matches
5. **Verify Results**: Cross-reference semantic search results with file system inspection when needed

### Integration with Development Workflow

- **Code Discovery**: Start with `search_code` to understand existing patterns
- **Implementation**: Use search results to find similar implementations as templates
- **Refactoring**: Use semantic search to find all related code before making changes
- **Documentation**: Search for existing documentation or comments about patterns
- **Testing**: Find test patterns and examples using semantic search

## GitHub CLI Integration

The GitHub CLI (`gh`) is essential for managing repository operations, monitoring CI/CD workflows, and handling pull requests efficiently.

### Key Commands

```bash
# Monitor workflow runs
gh run list --workflow="ci.yml"
gh run watch <run-id> --compact --exit-status --interval 5

# Manage pull requests
gh pr create --fill
gh pr checks <pr-number> --watch
gh pr view <pr-number> --json files --jq '.files[].path'

# Repository management
gh repo view
gh secret list
```

See [`docs/runbooks/repo-admin.md`](docs/runbooks/repo-admin.md) and [`docs/runbooks/github-workflow-enhancements.md`](docs/runbooks/github-workflow-enhancements.md) for comprehensive GitHub CLI usage.

## Research & Knowledge Management

### MCP Search Practice

- Triage query type first: use `mcp-router__brave_web_search` for broad web research, switching to `mcp-router__brave_news_search` when freshness (≤7 days) matters.
- For deep-dive investigations build a map→extract pipeline: `mcp-router__tavily_map` to enumerate relevant docs, then `mcp-router__tavily_extract` for full-text pulls.
- For library and framework-specific research, use `mcp-router__resolve-library-id` to find the correct Context7-compatible library ID, then `mcp-router__get-library-docs` to retrieve up-to-date documentation.
- For real-world code examples, leverage `mcp-router__searchGitHub` to find relevant code from over a million public repositories.
- For programming-related questions, leverage `mcp-router__get_code_context_exa` to find relevant context for APIs, libraries, and SDKs.
- Use `clink` to delegate tasks to external AICLIs when a task is better suited for another model's specific strengths.

### Quarterly Research Priorities

- Schedule quarterly version reviews using `mcp-router__brave_web_search` and `mcp-router__tavily_extract` to capture changelog highlights for Spring Boot, Kafka, Debezium, and Gradle.
- Evaluate OpenTelemetry tracing implementations and best practices for distributed systems.
- Research advanced Grafana dashboard patterns for microservices monitoring.
- Investigate service mesh integration with observability platforms.
- Review Debezium high-availability patterns and Spring Kafka EOS guidance.

## Knowledge Memory Practice

- Persist key decisions, architectural patterns, runbook updates, and milestone completions with `mcp-router__add-memory` using succinct, action-oriented phrasing; tag entries with the related doc path (`@AGENTS`, `@IMPLEMENTATION_PLAN`, etc.).
- Before starting research or planning work, run `mcp-router__search-memories` with focus keywords (e.g., `Kafka outbox`, `Debezium`, `clink`) to surface prior conclusions and avoid duplicate effort.
- Summaries captured in memory must also be reflected in the canonical docs (`@AGENTS.md`, `IMPLEMENTATION_PLAN.md`) so stored memories stay consistent with living documentation.
- When decisions change, append new memories referencing the superseded choice and update affected docs immediately; never delete prior memories—record reversals explicitly.
- Review memory entries during weekly syncs to spot outdated assumptions and schedule doc or implementation updates as needed.

## Code Style & Quality

- Adopt Spring's 2025 Java code style (Google-derived): UTF-8, LF endings, tab indentation, 120-character line target, and no trailing whitespace. Configure IDEs to honor the repo `.editorconfig`, and use `./gradlew ktlintFormat` to auto-format Kotlin sources; CI enforces `ktlintCheck` and `detekt` on every change.
- Use constructor injection for Spring-managed components, keep controllers/services package-private unless cross-module visibility is required, and leverage Java records or Lombok-free value classes for immutability at the edges.
- Separate domain, application, and adapter DTOs; map via MapStruct or dedicated translators to avoid leaking persistence or transport annotations into core logic.
- Layer tests: domain/application tests run without Spring (JUnit 5 + AssertJ); adapter tests rely on Testcontainers for Kafka/Postgres, and consumer/producer contract tests validate event schemas in `common-events`.
- Enforce static analysis and QA tooling: Detekt, ktlint, SpotBugs, Error Prone, dependency-vulnerability scanning, and Jacoco thresholds aligned with critical paths (domain/application ≥90%, adapters ≥75%). Security scanning includes OWASP Dependency Check, Trivy, and CodeQL.
- Caching strategy: adopt Redis (cache-aside/lazy population) for high-read workloads; standardize TTLs, invalidation hooks, and security hardening per Redis guidance. See [`docs/runbooks/cache.md`](docs/runbooks/cache.md).
- Content delivery: leverage CDN/edge caching (e.g., AWS CloudFront, Cloudflare) for static assets and API caching headers; integrate cache-busting into CI pipelines. See [`docs/runbooks/cdn.md`](docs/runbooks/cdn.md).

## DevOps & Delivery Practices

- Infrastructure-as-code (Terraform/Helm) governs Kubernetes, Istio, and gateway deployments; peer-review and lint all IaC changes in CI.
- CI/CD pipelines (GitHub Actions/GitLab) automate lint, tests, security scans, container builds, schema compatibility checks, and progressive delivery with canaries. See [`docs/runbooks/cicd-operations.md`](docs/runbooks/cicd-operations.md).
- Enhanced Security Scanning: Integrated OWASP Dependency Check, Trivy vulnerability scanner, and CodeQL static analysis with GitHub's security features.
- Dependency Management: Added dependency review with license compliance checking and vulnerability scanning through GitHub's Dependency Review Action.
- Infrastructure Validation: Implemented Docker Compose, Kubernetes, and Terraform configuration validation workflows.
- Static Analysis: Integrated SpotBugs and Error Prone for enhanced code quality assurance.
- Gradle builds must run with configuration cache and build cache enabled (`org.gradle.configuration-cache=true`, `org.gradle.caching=true`); CI invokes `./gradlew --configuration-cache` and developers should prefer the same for local workflows.
- Run `./gradlew schemaCompatibilityCheck` to validate Avro schemas before publishing; CI executes the task alongside `check`. See [`docs/runbooks/schema-registry.md`](docs/runbooks/schema-registry.md).
- Resilience engineering: run regular chaos drills (broker restarts, mesh failures, cache outages) and record findings in runbooks.

## Observability & Monitoring

The project implements comprehensive observability with metrics, distributed tracing, centralized logging, and health checks. See [`docs/observability.md`](docs/observability.md) for complete implementation details.

**Key Components**:
- **Metrics**: Prometheus for metrics collection with Micrometer instrumentation; Grafana dashboards for service performance and business metrics.
- **Distributed Tracing**: OpenTelemetry SDK integrated via `common-observability` module; context propagation across service boundaries; OpenTelemetry Collector and Jaeger backend deployed. See [`docs/runbooks/opentelemetry.md`](docs/runbooks/opentelemetry.md).
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana, Filebeat) for log aggregation and visualization. See [`docs/structured-logging-implementation-guide.md`](docs/structured-logging-implementation-guide.md).
- **Health Checks**: Service health endpoints with dependency status. See [`docs/runbooks/health-checks.md`](docs/runbooks/health-checks.md).

**Integration with Development**:
- Enforce OpenTelemetry instrumentation in all services.
- Centralize logs/metrics with configurable endpoints.
- Maintain dashboards/alerts for latency, errors, saturation, and business SLIs.
- Run chaos drills regularly to test monitoring resilience.

## Data Consistency Workflow

- Within each command handler, persist domain aggregates and append an outbox row inside one transaction; mark unsent events with `status='PENDING'`.
- Debezium connectors stream `PENDING` outbox rows to Kafka topics; configure Outbox SMT to map table columns to payload + headers (`eventId`, `schemaVersion`, tracing context). Polling relays are reserved for CDC-restricted environments and must follow the shared retry/backoff configuration (`maxAttempts=5`, exponential backoff starting at 500 ms).
- Consumers process messages idempotently: check a `processed_events` table keyed by `event_id` before invoking side effects; commit offsets only after success.
- Application services validate commands prior to persistence (non-blank customer identifiers, non-empty item collections) and reuse a single captured `Instant` for domain writes, Avro event timestamps, and outbox records to ensure envelope consistency.
- Maintain a shared `processed_events` ledger per service (JPA entity + Flyway migration) to short-circuit duplicate payloads inside Kafka listeners; wrap listener handlers in Spring transactions backed by `KafkaTransactionManager` so ledger writes, payment persistence, and offset commits share a unit of work.
- For long-running sagas, store state transitions in a dedicated table and emit compensating events on failure paths.
- Payment service delegates charging to `PaymentGateway` adapters; successful authorizations emit `PaymentCompletedEvent` while declines emit `PaymentFailedEvent` and mark the saga `FAILED`. The processed-event ledger is persisted within the same transaction so Kafka listener duplicates no longer re-trigger payments. Select the adapter via `payments.gateway.mode` (`IN_MEMORY` or `HTTP`); configure in-memory approval thresholds under `payments.gateway.in-memory.*` and external provider settings (`baseUrl`, `chargePath`, `apiKey`, timeouts, `retries`) under `payments.gateway.http.*`.
- Payment compensation writes refunds to the `refunds` ledger and emits `PaymentRefundedEvent` outbox records when saga rollback is invoked; duplicate compensation attempts reuse the completed refund record.
- Notification service supports email, SMS, and push channels via configurable sender adapters; channel properties allow simulate-failure toggles for tests while the retry template governs exponential backoff.
- Inventory service enforces stock availability via the pessimistic-locked `inventory_stock` table; `inventoryService.reserve` will throw when stock is insufficient and compensation (`release`) restores quantities before the saga transitions to `FAILED`.

## Saga Implementation Patterns

- Represent each saga as a state machine persisted in `sagas` tables (`saga_id`, `type`, `state`, `data`, `updated_at`); transitions are triggered by inbound events or timeouts. Use optimistic locking to prevent concurrent updates.
- Reuse `common-sagas` shared module for saga persistence: `SagaStateService` manages start/transition/complete/fail flows with optimistic locking and correlation-id uniqueness.
- Standardize saga step markers via `SagaStepNames` and the helper `SagaStepFormatter` so logs, monitoring, and replay tooling can parse step history consistently.
- Provide compensating helpers (`paymentsService.compensate`, `inventoryService.release`) that transition sagas to `COMPENSATING`/`FAILED` with clear step annotations (`payment-compensated`, `inventory-released`) while downstream actions (refunds, stock release) are stubbed for future integrations.
- **Adopt Temporal (self-hosted or cloud) as the orchestrator for complex, multi-domain sagas**. The implementation uses a distributed worker model:
  - Workflow definitions and implementations are centralized in `common-temporal` module for consistency and reusability
  - Each participating microservice (`payments-service`, `inventory-service`, `notification-service`) runs its own worker to process activities on dedicated task queues
  - Workflow orchestration integrates with existing service domain logic and maintains consistency with hexagonal architecture
  - Comprehensive error handling, metrics collection, and parallel compensation support for reliable distributed transactions
  - Type-safe activity interfaces with structured result types for better maintainability
- Emit compensating commands/events from the application layer or Temporal activities when a step fails; include correlation identifiers and reason codes so downstream services can reconcile partial changes.
- Provide idempotent handlers by combining processed-event ledgers with business keys (e.g., `order_id`); return early if the saga step has already completed.
- Document saga flows with sequence diagrams in [`docs/sagas/`](docs/sagas/) and include contract tests that replay happy-path, compensating, and timeout scenarios.
- Record step counters through `SagaMetricsRecorder` (`saga.step.processed` with `sagaType`, `step`, and `state` tags) and assert counter deltas in service tests to guard against double emission or missing compensations.

See [`docs/sagas/order-fulfillment.md`](docs/sagas/order-fulfillment.md) and [`docs/runbooks/temporal.md`](docs/runbooks/temporal.md) for detailed saga implementation guidance.

## Temporal Workflow Orchestration

The project implements **Temporal workflow orchestration** for complex, multi-domain business processes that require distributed transaction coordination and compensation patterns. See [`docs/runbooks/temporal.md`](docs/runbooks/temporal.md) for comprehensive documentation.

**Architecture**: Workflow definitions and implementations are centralized in `common-temporal` module. Each participating microservice runs its own activity worker on dedicated task queues.

**Key Features**:
- Saga pattern with parallel compensation
- Type-safe activity interfaces with structured result types
- Comprehensive error handling and metrics collection
- Integration with existing OpenTelemetry and metrics infrastructure

**Integration**: The Temporal implementation enhances rather than replaces existing patterns—activities publish events via existing outbox pattern, saga state changes are recorded in existing saga tables, and workflow orchestration respects microservice domain boundaries.

## Schema Evolution Policy

- Enforce backward compatibility for message schemas (Avro `BACKWARD` mode, JSON Schema additive changes only). Breaking changes require a new topic or schema version with dual-publish strategy.
- Automate schema validation in CI using `./gradlew schemaCompatibilityCheck`; merges fail if compatibility rules are violated.
- Record schema change intents in `docs/schemas/CHANGELOG.md` with effective dates, owner, and impacted services. Notify consumer teams via shared channels before rollout.
- For database schemas, apply Flyway migrations with reversible scripts where feasible and maintain downgrade instructions in the migration description.

See [`docs/adrs/0003-schema-governance.md`](docs/adrs/0003-schema-governance.md) and [`docs/runbooks/schema-registry.md`](docs/runbooks/schema-registry.md).

## Reliability & Operations

- Configure retry/DLQ topics per domain (`<topic>.retry`, `<topic>.dlq`) with exponential backoff handled in Spring Retry templates or Kafka Streams topologies.
- Monitor transaction aborts, outbox lag, Debezium lag, and consumer group lag via Micrometer + Prometheus; expose dashboards in Grafana.
- Propagate distributed tracing context via W3C Trace Context headers; integrate OpenTelemetry SDK in each service.
- Document standard incident runbooks under [`docs/runbooks/`](docs/runbooks/) (broker outage, Debezium connector restart, replay outbox).

## Operational Automation & Runbooks

All operational runbooks are maintained under [`docs/runbooks/`](docs/runbooks/):

- **API Gateway**: [`docs/runbooks/api-gateway.md`](docs/runbooks/api-gateway.md) - Route deployment, canary rules, rate-limit tuning, auth provider rotation, rollback steps
- **Debezium Connector**: [`docs/runbooks/debezium-connector.md`](docs/runbooks/debezium-connector.md) - Schema export, registry publication, replay tooling, troubleshooting
- **Service Mesh**: [`docs/runbooks/service-mesh.md`](docs/runbooks/service-mesh.md) - Istio configuration and troubleshooting
- **Polyglot Datastore**: [`docs/runbooks/polyglot-datastore.md`](docs/runbooks/polyglot-datastore.md) - Database operations and maintenance
- **OpenTelemetry**: [`docs/runbooks/opentelemetry.md`](docs/runbooks/opentelemetry.md) - Distributed tracing implementation
- **Temporal**: [`docs/runbooks/temporal.md`](docs/runbooks/temporal.md) - Workflow orchestration operations
- **Polling Relay**: [`docs/runbooks/polling-relay.md`](docs/runbooks/polling-relay.md) - Fallback outbox processing
- **Health Checks**: [`docs/runbooks/health-checks.md`](docs/runbooks/health-checks.md) - Service health monitoring
- **Platform**: [`docs/runbooks/platform.md`](docs/runbooks/platform.md) - General platform operations

## Security & Compliance

- Enforce TLS and SASL for Kafka brokers; manage ACLs so services only access their topics.
- Store credentials with Vault or AWS SecretsManager; never commit secrets. See [`docs/adrs/0004-secrets-management.md`](docs/adrs/0004-secrets-management.md) and [`docs/runbooks/vault.md`](docs/runbooks/vault.md).
- Classify events that carry PII; mask or tokenize sensitive fields before publishing.
- Kubernetes secrets: prefer Vault Agent Injector or Secrets Operator to deliver short-lived credentials; enforce RBAC and audit logging around secret access.

## Documentation Hygiene

- Keep `@IMPLEMENTATION_PLAN.md` synchronized with roadmap changes; link additional subsystem guides using `@docs/<name>.md`.
- Record schema evolution decisions in `docs/schemas/CHANGELOG.md`; require backward-compatible changes unless otherwise approved.
- Maintain [`docs/architecture/service-catalog.md`](docs/architecture/service-catalog.md), [`docs/notes/phase-0-*.md`](docs/notes/), and ADRs in [`docs/adrs/`](docs/adrs/) as the source of truth for discovery outcomes.
- Ensure [`docs/dev/getting-started.md`](docs/dev/getting-started.md) stays current with tooling and workflow changes.
- Keep runbooks up-to-date with implementation changes and regularly review for accuracy.

## Transactional Outbox Pattern

The project implements the transactional outbox pattern using both Debezium as the primary mechanism and a polling relay as a fallback. This ensures that domain data changes and event publications happen atomically within the same database transaction.

**Implementation Approaches**:
1. **Primary: Debezium CDC with Outbox Event Router SMT** - Low-latency, exactly-once delivery semantics
2. **Fallback: Polling Relay** - Scheduled service with REST endpoints for manual processing and replay

**Key Features**:
- Exactly-once semantics through Kafka transactions and Debezium's Outbox Event Router
- High availability with polling relay fallback
- Monitoring via metrics collection for pending messages, processed messages, and failures
- Replay capability for recovery scenarios
- Idempotency through processed events table
- Flexible routing based on aggregate type

See [`docs/adrs/0001-transactional-outbox.md`](docs/adrs/0001-transactional-outbox.md), [`docs/adrs/0002-debezium-outbox-adoptation.md`](docs/adrs/0002-debezium-outbox-adoptation.md), [`docs/adrs/0005-polling-relay-mechanism.md`](docs/adrs/0005-polling-relay-mechanism.md), [`docs/runbooks/debezium-connector.md`](docs/runbooks/debezium-connector.md), and [`docs/runbooks/polling-relay.md`](docs/runbooks/polling-relay.md) for detailed implementation guidance.
