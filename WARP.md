# Kafka Transactional Microservices – Agent Guide

## Scope & Outcomes

- Build Spring Boot microservices that coordinate business state changes with Kafka events without dual writes.
- Guarantee at-least-once delivery everywhere and exactly-once semantics for read→process→write flows that update a database and publish to Kafka.
- Keep `@IMPLEMENTATION_PLAN.md` current; treat it as the source for milestones, deliverables, and sequencing.

## Architectural Guardrails

- Apply the transactional outbox pattern so domain data and outbound events commit in the same RDBMS transaction; use Debezium Outbox SMT or a polling relay as the publisher.
- Wire business transactions through Spring for Apache Kafka using `KafkaTransactionManager` so DB work and Kafka offset commits succeed or roll back together.
- Enable idempotent Kafka producers (`enable.idempotence=true`, `acks=all`) and configure consumers with `isolation.level=read_committed`; persist idempotency keys to shield downstream side effects.
- Configure Kafka consumers with proper transaction management by setting `containerProperties.kafkaAwareTransactionManager` instead of the deprecated `transactionManager` property.
- Keep saga choreography lightweight: prefer domain events plus compensating actions over distributed 2PC; reserve orchestration for cross-domain long-running transitions.

## Library & Framework Practices

- Before introducing new libraries or implementing features, research existing usage patterns in the codebase first
- Use documentation tools (Context7, DeepWiki, GitHub search) to understand proper library usage before implementation
- Follow existing patterns in shared modules for consistency across services
- Verify that new library dependencies align with the established tech stack and version matrix
- Prefer Spring Boot auto-configuration patterns over custom configurations where available
- Maintain dependency compatibility across all modules as documented in `docs/version-matrix.md`

## Version Management Practices

- Regularly check for newer versions of key dependencies using authoritative sources:
  - MavenCentral Repository (<https://mvnrepository.com/>) for Java/Kotlin libraries
  - Gradle Plugin Portal (<https://plugins.gradle.org/>) for Gradle plugins
  - Google's Maven Repository (<https://maven.google.com/web/index.html>) for Android libraries
  - Official project release pages and GitHub repositories
- When evaluating version upgrades, consider:
  - Compatibility with existing dependencies
  - Security fixes and vulnerability patches
  - Performance improvements and new features
  - Migration effort and breaking changes
  - LTS vs. latest version tradeoffs
- Document version decisions in `docs/version-matrix.md` with rationale for selections
- Run comprehensive tests (unit, integration, contract) after version upgrades
- Update the Gradle version catalog (`gradle/libs.versions.toml`) with new versions following semantic versioning conventions
- Enforce the use of the Gradle version catalog for all plugins and dependencies in build files to ensure consistent version management across all modules

## Architecture Blueprint

- Apply Hexagonal (Ports & Adapters) layering inside every service: domain/core modules contain entities, value objects, and domain services; application modules expose use-cases and transaction boundaries; adapters implement inbound transports (REST, gRPC, Kafka listeners) and outbound integrations (repositories, downstream APIs). This isolates business logic from frameworks and eases testing.
- Provide shared gRPC and message contracts via the `common-proto` module; services should import generated stubs instead of defining ad-hoc protos.
- Treat each microservice as a bounded context following Domain-Driven Design. Align aggregates and repositories to business capabilities, avoid cross-context entity reuse, and surface integration exclusively through stable APIs or domain events.
- Package code by feature slice (`orders.application`, `orders.adapter.outbound.events`) rather than by technical tier to keep related classes co-located while respecting hexagonal layers. Enforce module boundaries with Gradle conventions and restricted visibility.
- Centralize edge concerns in an API gateway (Spring Cloud Gateway or equivalent) for routing, authN/Z, rate limiting, schema validation, and trace propagation. Downstream services stay focused on domain logic and publish events for cross-service workflows.
- Reserve synchronous service-to-service calls for bounded contexts that truly need request/response semantics; when required, standardize on gRPC with Protobuf IDLs so contracts remain type-safe while Avro continues to back Kafka event streams. Document mappings between Protobuf DTOs and Avro domain events to avoid drift.
- Inventory service exposes the `inventory.v1.StockReconciliationService` gRPC endpoint (configurable via `inventory.grpc.port`) for bulk stock adjustments; enable/disable access through gateway routing and ensure synchronous clients honor saga invariants.
- Keep infrastructure concerns (messaging clients, persistence configs, observability) in dedicated adapter modules to make technology swaps (e.g., Kafka → Pulsar) incremental without rewriting the domain core.

## Service Connectivity Strategy

- North-south traffic: Spring Cloud Gateway remains the dedicated edge gateway, handling authentication, rate limiting, request/response shaping, and contract enforcement before requests reach internal meshes.
- East-west traffic: Deploy Istio 1.24+ in ambient mode for sidecar-less service mesh capabilities (mTLS, traffic policy, zero-trust, observability) while retaining compatibility with Kubernetes Gateway API. Capture mesh configuration under `infra/istio/` and automate via Helm/Terraform.
- Integration approach: Gateway routes terminate at mesh ingress gateways; Istio manages service-to-service policy, retries, and telemetry. Use Ambient waypoints for L7 policies on critical namespaces.
- Multi-cluster readiness: Track Istio ambient multicluster progress (1.27+ features) and prototype failover scenarios before production adoption.

## Platform Baseline

- Languages & runtimes: Kotlin 2.2.20 as the primary implementation language running on Java 25 (latest GA) with Java 23 and Java 21 retained as fallbacks. Enable preview JVM features only behind build profiles and document promotion decisions. Keep Kotlin `jvmTarget` at 21 until the compiler adds bytecode support beyond Java 24.
- Frameworks: Spring Boot 3.5.6 with Spring Framework 6.2.11 and Spring for Apache Kafka 3.3.10 (EOS v2); monitor 3.5.x patch releases and prepare for Spring Boot 4.0 milestones as needed.
- Build & tooling: Gradle 9.1.0 (Kotlin DSL), Testcontainers 1.20+, Docker Engine 27+, and OpenTelemetry SDK 1.44+ baked into the shared parent project.
- Serialization & JSON: Keep Spring Boot 3.5.x defaults and pin `com.fasterxml.jackson` artifacts to 2.20.0 for security fixes; use Kotlinx Serialization (`Json.Default`) to build structured payload fragments before embedding them in Avro event envelopes.
- Messaging: Apache Kafka 4.1.0 with pure KRaft metadata mode (no ZooKeeper dependency) or Confluent Platform 8.x with Schema Registry; set `transaction.state.log.replication.factor >= 3`, `transaction.max.timeout.ms <= 900000` (15m), and `min.insync.replicas >= 2`.
- Data: PostgreSQL 15+ (logical replication enabled) or MySQL 8.0.32+; define `outbox` tables per bounded context with JSON payload + metadata fields (`event_id`, `aggregate_type`, `aggregate_id`, `occurred_at`). Note alternative datastores (document DBs, key-value) in service ADRs and justify integration approach (CDC bridge, change-projection service, etc.).
- CDC: Debezium 3.3.0.Final connectors with Outbox Event Router SMT are the default outbox publisher; maintain connector manifests under `infra/debezium/` and adhere to platform backup/runbook requirements. A lightweight polling relay may be enabled only when CDC access is unavailable—record the exception in the service ADR and implement the retry/backoff settings defined in `@IMPLEMENTATION_PLAN.md`.
- Serialization: Avro or JSON Schema enforced through the registry; version message contracts via Gradle module `common-events`. Avoid direct Jackson dependencies in shared modules—wire actual serializers (Spring Kafka JSON, Avro, or Kotlinx) per service adapter when needed.

## Version & Compatibility Policy

- Default to the latest stable GA releases: Java 25, Spring Boot 3.5.x, Spring for Apache Kafka 3.3.x, Kafka 4.1.x, Debezium 3.3.x, Gradle 9.1.x. Record the prior LTS versions in `docs/version-matrix.md` as fallbacks with downgrade guidance.
- Maintain `docs/version-matrix.md` listing each service's current, candidate, and fallback versions (JDK, Spring Boot, Kafka client, Debezium connector, Testcontainers) plus compatibility notes.
- Run a quarterly dependency review (see Research Backlog) to validate new maintenance drops; require smoke tests, upgrade playbooks, and rollback plans before updating production baselines.
- Use CI checks (Gradle task `versionCheck`) to flag mismatched runtime versions across services; merge is blocked until the matrix is updated or the mismatch is resolved.

## Docker & Container Image Management

### Docker CLI Version Verification

When managing Docker infrastructure components, use the Docker CLI directly to verify latest versions rather than relying solely on web searches.

#### Core Commands

```bash
# Search for official repositories
docker search --limit 5 postgres
docker search --limit 5 redis
docker search --limit 5 apache/kafka

# Query Docker Hub API directly for accurate tags
curl -s "https://registry.hub.docker.com/v2/repositories/library/postgres/tags?page_size=10" | jq -r '.results[] | "\(.name): \(.last_updated)"'

# Verify multi-architecture support
docker manifest inspect redis:latest
```

#### Best Practices

1. **Prefer Official Images**: Use `docker search` to identify official repositories marked with `[OK]`
2. **Version Selection**: Use major version tags (e.g., `postgres:18`, `redis:8-alpine`), avoid `latest` in production
3. **Cross-Reference Sources**: Combine Docker CLI results with Docker Hub API calls and local cache verification
4. **Environment Management**: Maintain image versions in `infra/.env` and use environment variables in `compose.yml`

#### Verification Workflow

```bash
# Research new versions
docker search <service_name>
curl -s "https://registry.hub.docker.com/v2/repositories/library/<service>/tags?page_size=10" | jq '.results[0:3] | .name'
docker manifest inspect <image>:<tag>
```

## Dev Workflow & Commands

- Bootstrap infra with env: `cp infra/.env.example infra/.env && docker compose --env-file infra/.env -f infra/compose.yml up -d`.
- Run all service tests with `./gradlew clean test` and integration tests with `./gradlew :service-* :integration-test` once modules exist.
- When you need to run shell commands, prefer the `execute_terminal_command` MCP tool so terminal interactions stay auditable and repeatable.
- Start a sample service locally via `./gradlew :orders-service:bootRun` after loading `.env` (copy from `.env.example` and use direnv or `source scripts/export-env.sh`).
- Use `./gradlew flywayMigrate` to apply schema migrations before running services.
- Lint/format with `./gradlew spotlessApply` (add plugin in the build once codebase is scaffolded).
- When editing or inspecting code via JetBrains MCP server, open the target file with `open_file_in_editor` before running `get_file_problems` so IntelliJ indexes the file, then review errors/warnings ahead of Gradle tasks.
- Export Avro schemas with `./gradlew exportAvroSchemas` and publish via `scripts/schema-publish.sh` before enabling Debezium connectors; registry compatibility is enforced in CI.
- Service modules live under `services/<name>` (e.g., `orders-service`) and follow the hexagonal template documented in `docs/dev/service-template.md`; depend on shared modules for events, Kafka, persistence, sagas, and observability.
- Launch disposable CLI subagents with `clink` when fresh context windows are needed for specific tasks. Currently, only claude, codex, and gemini are supported by clink due to a hardcoded allowlist. For tasks requiring other CLIs like qwen, temporarily remap an existing client or use the CLI directly.
- Always use the Gradle version catalog (`gradle/libs.versions.toml`) when adding new plugins or dependencies to build files to ensure consistent version management across all modules
- **Local Workflow & Document Validation**: Before committing changes, especially to GitHub Actions workflows or documentation, run local validation tools to catch errors early. These tools are typically installed via package managers like `npm`, `pip`, or `brew`.

  **Post-Edit Validation Practice**: After editing any `.yml`, `.yaml`, or `.md` files, always run the appropriate linting tools to ensure quality and consistency:

  ```bash
  # After editing YAML files (.yml, .yaml)
  yamllint <filename>.yml                    # Check single file
  yamllint .                                 # Check all YAML files in project
  yamllint infra/ .github/                   # Check specific directories

  # After editing Markdown files (.md)
  npx markdownlint <filename>.md             # Check single file
  npx markdownlint "**/*.md"                 # Check all markdown files
  npx markdownlint docs/**/*.md              # Check specific directories

  # After editing GitHub Actions workflows
  actionlint .github/workflows/*.yml         # Check workflow files
  ```

  **Automatic Fixing Workflow**: Many linting issues can be automatically fixed:

  ```bash
  # Auto-fix markdownlint issues
  npx markdownlint "**/*.md" --fix

  # Auto-fix YAML formatting issues (requires yamllint config)
  yamllint -d relaxed .github/workflows/*.yml

  # Format Kotlin code (after editing .kt files)
  ./gradlew ktlintFormat
  ```

  **Comprehensive Validation Commands**:

  ```bash
  # Validate all documentation and configuration files
  make lint-all                             # Custom make command (if available)
  npx markdownlint "**/*.md" --fix && yamllint . --no-warnings

  # Check specific file types
  find . -name "*.yml" -o -name "*.yaml" | xargs yamllint --no-warnings
  find . -name "*.md" -not -path "./build/*" | xargs npx markdownlint --fix
  ```

  **Tool Installation**:

  ```bash
  # Install required tools (macOS)
  brew install yamllint actionlint
  npm install -g markdownlint-cli

  # Or use npx for markdownlint without global installation
  # npx markdownlint is recommended for project consistency
  ```

  **Integration with Development Workflow**:

    1. **After any file edit**: Run the appropriate linter immediately
    2. **Before commits**: Run comprehensive lint check across all modified files
    3. **CI/CD alignment**: Use the same tools and configurations as CI pipelines
    4. **Error handling**: Review and fix all linting issues before pushing changes

  **Common Linting Issues and Fixes**:

  - **YAML**: Line length (>80 chars), indentation, trailing whitespace
  - **Markdown**: Missing fence languages, list formatting, heading duplication
  - **GitHub Actions**: Outdated actions, missing required fields, syntax errors

## Database & CDC Setup (Standardized Docker Compose)

- Local baseline uses PostgreSQL 18 with logical decoding enabled; Compose mounts init scripts under `infra/postgres/init` to create service DBs (orders, payments, inventory, notification) and `pgcrypto`.
- Schema is codified with Flyway migrations per service (`services/*/src/main/resources/db/migration`). A dedicated Compose profile `migrate` runs four one-off Flyway containers:
  - flyway-orders, flyway-payments, flyway-inventory, flyway-notification - Apply with: `make migrate`
- Kafka 4.1 (KRaft) is used locally. Healthcheck calls the bundled broker tool. Auto-create topics is disabled for parity with production; the internal `__consumer_offsets` topic is created explicitly.
- Debezium Connect 3.3 is the CDC default:
  - Connectors: orders, payments, inventory, notification
  - `topic.prefix` set per DB, `snapshot.mode=no_data` (3.x compliant)
  - Outbox Event Router routes to `outbox.${routedByValue}` with key=`aggregate_id`
  - `transforms.outbox.table.fields.additional.placement` excludes payload to avoid schema duplication
- Connector-side `topic.creation.default.*` is enabled for local dev so outbox topics are created when producing
- Outbox table is "lean Debezium-only" (no status column). If enabling a custom outbox relay, add `status` via a migration.
- Standardized workflows:
  - Start local stack: `make up`
  - Run migrations: `make migrate`
  - Register/refresh connectors: `make connectors`
  - Smoke test (insert outbox row → read from Kafka): `make smoke`
  - Tear down: `make down` (or `make clean-volumes` for a full reset)

## Documentation & Context Best Practices

- Before adding or editing code, use documentation tools to understand existing patterns and libraries:
  - Use `get_code_context_exa` to search for relevant API/library contexts before implementing new features
  - Use `searchGitHub` to find real-world examples and implementation patterns from similar projects
  - Use `resolve-library-id` and `get-library-docs` to access up-to-date library documentation via Context7
  - Use `read_wiki_structure` and `read_wiki_contents` to explore GitHub repository documentation
  - Use DeepWiki for comprehensive documentation exploration before implementing complex features
- Always verify implementation approaches against existing code patterns in the repository before writing new code
- When using external libraries, first research their correct usage patterns and configuration through documentation tools
- Follow established patterns in shared modules (`common-*`) as templates for new implementations

## GitHub CLI Integration

The GitHub CLI (`gh`) is essential for managing repository operations, monitoring CI/CD workflows, and handling pull requests efficiently. Below are common practices and commands for the microservices project.

### Installation & Authentication

```bash
# Install GitHub CLI (macOS)
brew install gh

# Authenticate with GitHub (choose HTTPS + browser or PAT)
gh auth login

# Confirm scopes and active host
gh auth status

# Reuse credentials for git fetch/push
gh auth setup-git

# Set default editor for PR descriptions and issues
gh config set editor <editor-name>

# Optional: configure frequently used aliases
gh alias set prd 'pr create --draft'
```

> **Token hygiene**: when scripting, export `GH_TOKEN`/`GITHUB_TOKEN` with minimal scopes (for CI logs use `actions:read`; reruns need `actions:write`). Rotate PATs alongside other credentials.

### Monitoring Actions & Workflows

**View recent workflow runs:**

```bash
# List recent workflow runs
gh run list

# View runs for a specific workflow
gh run list --workflow="ci.yml"

# View detailed information about a specific run and fail the shell on errors
gh run view <run-id> --exit-status

# View logs for a specific job and only show failed steps
gh run view <run-id> --log --job=<job-name> --log-failed

# Watch a running workflow in real-time
gh run watch <run-id> --compact --exit-status --interval 5

# Emit structured data for dashboards / scripts
gh run view <run-id> --json conclusion,workflowName,url --jq '.workflowName + " → " + .conclusion'
```

**Workflow debugging and troubleshooting:**

```bash
# Download artifacts from a workflow run
gh run download <run-id> --dir artifacts/ --pattern '*report*'

# Rerun a failed workflow
gh run rerun <run-id> --failed

# Rerun a specific job (obtain job databaseId from --json jobs)
gh run rerun <run-id> --job <job-database-id>

# Cancel a running workflow
gh run cancel <run-id>

# Trigger workflow_dispatch with parameters
gh workflow run path/to/workflow.yml --ref main -f smoke=true

# Fallback when run names break log downloads: fetch archive directly
gh api repos/<owner>/<repo>/actions/runs/<run-id>/logs > run-logs.zip

# Enable debug logging (requires repository variables or rerun option)
gh run rerun <run-id> --debug
```

### Pull Request Management

**Create and manage pull requests:**

```bash
# Create a pull request from current branch
gh pr create

# Create PR with title and body
gh pr create --title "feat: add transactional outbox" --body "Implement outbox pattern for orders service"

# List pull requests
gh pr list

# View PR details and status checks
gh pr view <pr-number>

# Check PR status and CI results
gh pr checks <pr-number> --watch

# Merge a PR (with various merge methods)
gh pr merge <pr-number> --merge
gh pr merge <pr-number> --squash
gh pr merge <pr-number> --rebase
```

**PR review and collaboration:**

```bash
# Request review from team members
gh pr edit <pr-number> --add-reviewer vicle1908

# Add labels to PR
gh pr edit <pr-number> --add-label "dependencies"

# Approve a PR
gh pr review <pr-number> --approve

# Request changes
gh pr review <pr-number> --request-changes

# View PR diff
gh pr diff <pr-number>

# View files changed in PR
gh pr view <pr-number> --json files --jq '.files[].path'

# Apply PR locally for testing without switching branches
gh pr checkout <pr-number>
```

### Issue Management

**Create and manage issues:**

```bash
# Create an issue
gh issue create --title "Bug: Kafka consumer not processing messages" --body "Detailed description..."

# List issues
gh issue list

# View issue details
gh issue view <issue-number>

# Assign issue to yourself
gh issue edit <issue-number> --assignee vicle1908

# Add labels to issue
gh issue edit <issue-number> --add-label "bug,kafka"
```

### Repository Management

**View repository information:**

```bash
# View repository overview
gh repo view

# View repository with specific fields
gh repo view --json name,description,defaultBranch,createdAt

# List repository collaborators
gh repo list --json owner,name

# View repository traffic
gh repo view --json cloneTraffic,viewTraffic
```

### Configuration & Settings

**Manage repository and branch settings:**

```bash
# View repository settings
gh repo edit --json settings

# Set default branch
gh repo edit --default-branch main

# Manage branch protection rules (requires GitHub CLI v2.0+)
gh api repos/:owner/:repo/branches/:branch/protection

# View repository secrets (requires admin access)
gh secret list

# Set repository environment secrets
gh secret set ENV_VAR_NAME --body "secret_value"
```

### Project-Specific Workflows

**Common patterns for this microservices project:**

1. **Monitor CI/CD pipelines after Dependabot updates:**

   ```bash
   # Watch Dependabot PR builds
   gh pr list --author "app/dependabot[bot]"
   gh pr checks <pr-number>
   ```

2. **Verify deployment pipelines:**

   ```bash
   # Check deployment workflow status
   gh run list --workflow="deploy.yml"
   gh run view <run-id> --log
   ```

3. **Review dependency updates:**

   ```bash
   # Review Dependabot PR changes
   gh pr diff --repo vicle1908/kafka-transactional-microservices <pr-number>
   ```

4. **Security and compliance:**

   ```bash
   # View security scan results
   gh run list --workflow="security.yml"

   # Check code scanning alerts
   gh code scanning list
   ```

### Integration with Development Workflow

**Pre-commit checks:**

```bash
# Run local checks before pushing
make lint
make test

# Push and create PR with checks
git push -u origin feature-branch
gh pr create --fill --head feature-branch
```

**Post-merge cleanup:**

```bash
# Delete merged branches locally
git branch --merged | grep -v main | xargs git branch -d

# Sync with remote and clean up
git fetch origin --prune
```

### Automation Script Examples

**Batch operations for multiple PRs:**

```bash
# View all open Dependabot PRs with their status
gh pr list --author "app/dependabot[bot]" --json number,state,title,mergeable --jq '.[] | "\(.number): \(.title) - \(.state) (mergeable: \(.mergeable))"'

# Approve all passing Dependabot PRs
gh pr list --author "app/dependabot[bot]" --json number --jq '.[].number' | xargs -I {} gh pr review {} --approve
```

**Workflow monitoring scripts:**

```bash
# Monitor CI status for current PR
current_pr=$(gh pr view --json number --jq '.number')
gh pr checks $current_pr --watch

# Get workflow duration statistics
gh run list --json databaseId,status,conclusion,createdAt,completedAt --jq '.[] | select(.status == "completed") | "\(.databaseId): \(.conclusion) - duration: \((.completedAt | fromdateiso8601) - (.createdAt | fromdateiso8601)) seconds"'
```

### GitHub CLI Best Practices

1. **Always authenticate before use**: `gh auth login`
2. **Use PR templates**: Leverage `.github/PULL_REQUEST_TEMPLATE.md`
3. **Monitor CI closely**: Use `gh run watch` for long-running workflows
4. **Review Dependabot PRs promptly**: Automated dependencies need timely review
5. **Use meaningful commit messages**: They become PR titles with `gh pr create --fill`
6. **Leverage GitHub Actions**: Automate repetitive tasks with workflows
7. **Stay updated**: `gh --version` and `gh help` for latest features

### Integration with Claude Code

When working with Claude Code, you can use GitHub CLI commands to:

- Verify workflow runs after making changes
- Check PR status before starting new work
- Review merge conflicts and resolve them
- Monitor deployment pipelines after infrastructure changes
- Validate configuration changes in CI/CD workflows

The GitHub CLI is particularly valuable for this microservices project where frequent dependency updates, multiple services, and complex CI/CD pipelines require efficient repository management.

## Research & Knowledge Management

### MCP Search Practice

- Triage query type first: use `mcp-router__brave_web_search` for broad web research, switching to `mcp-router__brave_news_search` when freshness (≤7 days) matters and `mcp-router__brave_image_search` for visual assets.
- For deep-dive investigations build a map→extract pipeline: `mcp-router__tavily_map` to enumerate relevant docs, then `mcp-router__tavily_extract` for full-text pulls; request `include_raw_content` when evaluating technical specs.
- For library and framework-specific research, use `mcp-router__resolve-library-id` to find the correct Context7-compatible library ID, then `mcp-router__get-library-docs` to retrieve up-to-date documentation.
- For real-world code examples, leverage `mcp-router__searchGitHub` to find relevant code from over a million public repositories.
- For programming-related questions, leverage `mcp-router__get_code_context_exa` to find relevant context for APIs, libraries, and SDKs with the highest quality and freshest context.
- Use `mcp-router__consensus` for complex decisions to gather multiple AI perspectives, then apply `mcp-router__thinkdeep` when deeper reasoning is required.
- Use `clink` to delegate tasks to external AICLIs when a task is better suited for another model's specific strengths.

### Quarterly Research Priorities

- Schedule quarterly version reviews using `mcp-router__brave_web_search` and `mcp-router__tavily_extract` to capture changelog highlights for Spring Boot, Kafka, Debezium, and Gradle
- Evaluate OpenTelemetry tracing implementations and best practices for distributed systems
- Research advanced Grafana dashboard patterns for microservices monitoring
- Investigate service mesh integration with observability platforms
- Review Debezium high-availability patterns and Spring Kafka EOS guidance

### Key References

- Spring Kafka exactly-once & transactions documentation
- Confluent Platform release notes (KRaft-first transactions & licensing updates)
- Debezium release notes and outbox pattern implementations
- OpenTelemetry documentation and implementation guides
- Istio service mesh documentation for ambient mode
- ELK stack documentation for centralized logging

## Knowledge Memory Practice

- Persist key decisions, architectural patterns, runbook updates, and milestone completions with `mcp-router__add-memory` using succinct, action-oriented phrasing; tag entries with the related doc path (`@AGENTS`, `@IMPLEMENTATION_PLAN`, etc.).
- Before starting research or planning work, run `mcp-router__search-memories` with focus keywords (e.g., `Kafka outbox`, `Debezium`, `clink`) to surface prior conclusions and avoid duplicate effort.
- Summaries captured in memory must also be reflected in the canonical docs (`@AGENTS.md`, `IMPLEMENTATION_PLAN.md`) so stored memories stay consistent with living documentation.
- When decisions change, append new memories referencing the superseded choice and update affected docs immediately; never delete prior memories—record reversals explicitly.
- Review memory entries during weekly syncs to spot outdated assumptions and schedule doc or implementation updates as needed.
- Record clink usage patterns, limitations, and workarounds with `mcp-router__add-memory` to build institutional knowledge about CLI tool orchestration.

## Code Style & Quality

- Adopt Spring's 2025 Java code style (Google-derived): UTF-8, LF endings, tab indentation, 120-character line target, and no trailing whitespace. Configure IDEs to honor the repo `.editorconfig`, and use `./gradlew ktlintFormat` to auto-format Kotlin sources; CI enforces `ktlintCheck` and `detekt` on every change.
- Use constructor injection for Spring-managed components, keep controllers/services package-private unless cross-module visibility is required, and leverage Java records or Lombok-free value classes for immutability at the edges.
- Separate domain, application, and adapter DTOs; map via MapStruct or dedicated translators to avoid leaking persistence or transport annotations into core logic.
- Layer tests: domain/application tests run without Spring (JUnit 5 + AssertJ); adapter tests rely on Testcontainers for Kafka/Postgres, and consumer/producer contract tests validate event schemas in `common-events`.
- Enforce static analysis and QA tooling: Detekt, ktlint, SpotBugs, Error Prone, dependency-vulnerability scanning, and Jacoco thresholds aligned with critical paths (domain/application ≥90%, adapters ≥75%). Security scanning includes OWASP Dependency Check, Trivy, and CodeQL.
- Caching strategy: adopt Redis (cache-aside/lazy population) for high-read workloads; standardize TTLs, invalidation hooks, and security hardening per Redis guidance.
- Content delivery: leverage CDN/edge caching (e.g., AWS CloudFront, Cloudflare) for static assets and API caching headers; integrate cache-busting into CI pipelines.

## DevOps & Delivery Practices

- Infrastructure-as-code (Terraform/Helm) governs Kubernetes, Istio, and gateway deployments; peer-review and lint all IaC changes in CI.
- CI/CD pipelines (GitHub Actions/GitLab) automate lint, tests, security scans, container builds, schema compatibility checks, and progressive delivery with canaries.
- **Enhanced Security Scanning**: Integrated OWASP Dependency Check, Trivy vulnerability scanner, and CodeQL static analysis with GitHub's security features.
- **Dependency Management**: Added dependency review with license compliance checking and vulnerability scanning through GitHub's Dependency Review Action.
- **Infrastructure Validation**: Implemented Docker Compose, Kubernetes, and Terraform configuration validation workflows.
- **Static Analysis**: Integrated SpotBugs and Error Prone for enhanced code quality assurance.
- Gradle builds must run with configuration cache and build cache enabled (`org.gradle.configuration-cache=true`, `org.gradle.caching=true`); CI invokes `./gradlew --configuration-cache` and developers should prefer the same for local workflows.
- Run `./gradlew schemaCompatibilityCheck` to validate Avro schemas before publishing; CI executes the task alongside `check`.
- Resilience engineering: run regular chaos drills (broker restarts, mesh failures, cache outages) and record findings in runbooks.

## Observability & Monitoring

### Current Implementation

The project implements comprehensive observability with the following components:

**Metrics & Dashboards**:

- Prometheus for metrics collection with Micrometer instrumentation
- Grafana dashboards for service performance and business metrics
- Pre-built dashboards for Kafka, databases, and application metrics

**Distributed Tracing**:

- OpenTelemetry SDK integrated via `common-observability` module
- Context propagation across service boundaries
- Configuration for OpenTelemetry collector and Jaeger backend

**Health Checks**:

- Service health endpoints with dependency status
- Dedicated health check runbooks and monitoring procedures

### Implementation Roadmap

#### Phase 1: Centralized Logging (ELK Stack)

- Add Elasticsearch, Logstash, Kibana to `infra/compose.yml`
- Configure Filebeat for log shipping from services
- Create dashboards for service logs, error patterns, and performance analysis

#### Phase 2: Enhanced Tracing

- Deploy OpenTelemetry Collector and Jaeger
- Implement cross-service tracing (HTTP, Kafka, database)
- Integrate trace data with existing dashboards

### Integration with Development

- Enforce OpenTelemetry instrumentation in all services
- Centralize logs/metrics with configurable endpoints
- Maintain dashboards/alerts for latency, errors, saturation, and business SLIs
- Run chaos drills regularly to test monitoring resilience

## Data Consistency Workflow

- Within each command handler, persist domain aggregates and append an outbox row inside one transaction; mark unsent events with `status='NEW'`.
- Debezium connectors stream `NEW` outbox rows to Kafka topics; configure Outbox SMT to map table columns to payload + headers (`eventId`, `schemaVersion`, tracing context). Polling relays are reserved for CDC-restricted environments and must follow the shared retry/backoff configuration (`maxAttempts=5`, exponential backoff starting at 500 ms).
- Consumers process messages idempotently: check a `processed_events` table keyed by `event_id` before invoking side effects; commit offsets only after success.
- Application services validate commands prior to persistence (non-blank customer identifiers, non-empty item collections) and reuse a single captured `Instant` for domain writes, Avro event timestamps, and outbox records to ensure envelope consistency.
- Mirror the orders pattern in `payments-service`: compute payment payloads via Kotlinx Serialization, emit `PaymentCompletedEvent` Avro records, and persist payment aggregates as `COMPLETED` alongside outbox entries within one transaction; Kafka consumer wiring follows once topic contracts are defined.
- Maintain a shared `processed_events` ledger per service (JPA entity + Flyway migration) to short-circuit duplicate payloads inside Kafka listeners; wrap listener handlers in Spring transactions backed by `KafkaTransactionManager` so ledger writes, payment persistence, and offset commits share a unit of work.
- Inventory service consumes `PaymentCompletedEvent` messages, resolves stock reservations via `InventoryService.reserve`, and publishes `InventoryReservedEvent` payloads through the transactional outbox to notify downstream consumers.
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
- Adopt Temporal (self-hosted or cloud) as the orchestrator for complex, multi-domain sagas. The implementation uses a distributed worker model:
  - A dedicated workflow service (`temporal-pilot`) hosts the workflow logic, ensuring the orchestrator is isolated from other service deployments.
  - Each participating microservice (`payments-service`, `inventory-service`, etc.) runs its own worker to process activities on a dedicated task queue.
- Emit compensating commands/events from the application layer or Temporal activities when a step fails; include correlation identifiers and reason codes so downstream services can reconcile partial changes.
- Provide idempotent handlers by combining processed-event ledgers with business keys (e.g., `order_id`); return early if the saga step has already completed.
- Document saga flows with sequence diagrams in `docs/sagas/` and include contract tests that replay happy-path, compensating, and timeout scenarios.
- Record step counters through `SagaMetricsRecorder` (`saga.step.processed` with `sagaType`, `step`, and `state` tags) and assert counter deltas in service tests to guard against double emission or missing compensations.

## Schema Evolution Policy

- Enforce backward compatibility for message schemas (Avro `BACKWARD` mode, JSON Schema additive changes only). Breaking changes require a new topic or schema version with dual-publish strategy.
- Automate schema validation in CI using `./gradlew schemaCompatibilityCheck`; merges fail if compatibility rules are violated.
- Record schema change intents in `docs/schemas/CHANGELOG.md` with effective dates, owner, and impacted services. Notify consumer teams via shared channels before rollout.
- For database schemas, apply Flyway migrations with reversible scripts where feasible and maintain downgrade instructions in the migration description.

## Reliability & Operations

- Configure retry/DLQ topics per domain (`<topic>.retry`, `<topic>.dlq`) with exponential backoff handled in Spring Retry templates or Kafka Streams topologies.
- Monitor transaction aborts, outbox lag, Debezium lag, and consumer group lag via Micrometer + Prometheus; expose dashboards in Grafana.
- Propagate distributed tracing context via W3C Trace Context headers; integrate OpenTelemetry SDK in each service.
- Document standard incident runbooks under `docs/runbooks/` (broker outage, Debezium connector restart, replay outbox).

## Operational Automation & Gateway Runbooks

- Store API gateway playbooks under `docs/runbooks/api-gateway.md`, covering route deployment, canary rules, rate-limit tuning, auth provider rotation, and rollback steps. Integrate static contract checks to ensure gateway transformers match downstream schemas.
- Define CI/CD pipelines (GitHub Actions templates under `.github/workflows/`) that run ktlint, detekt, schema compatibility checks, unit and integration tests, plus container image builds with vulnerability scanning.
- Maintain Debezium connector runbook (`docs/runbooks/debezium.md`) covering schema export (`./gradlew exportAvroSchemas`), registry publication, and replay tooling (`scripts/outbox-replay.sh`).
- Automate Debezium connector lifecycle using Infrastructure-as-Code (Terraform/Helm) alongside smoke tests that validate lag and schema mappings after each deployment.
- For services using non-relational stores or polyglot runtimes, include adapter-specific health checks and replication monitoring in their runbooks; reference the shared version matrix to confirm driver compatibility.
- Maintain comprehensive observability runbooks for all monitoring components:
  - Service Mesh runbook (`docs/runbooks/service-mesh.md`) covering Istio configuration and troubleshooting
  - Polyglot Datastore runbook (`docs/runbooks/polyglot-datastore.md`) covering database operations and maintenance
  - OpenTelemetry runbook (`docs/runbooks/opentelemetry.md`) covering distributed tracing implementation

## Security & Compliance

- Enforce TLS and SASL for Kafka brokers; manage ACLs so services only access their topics.
- Store credentials with Vault or AWS SecretsManager; never commit secrets.
- Classify events that carry PII; mask or tokenize sensitive fields before publishing.
- Kubernetes secrets: prefer Vault Agent Injector or Secrets Operator to deliver short-lived credentials; enforce RBAC and audit logging around secret access.

## Immediate Follow-up Actions

- Author `docs/version-matrix.md` and implement the Gradle `versionCheck` task that validates runtime dependencies before merge.
- Build an end-to-end saga pilot (Order → Payment → Inventory) and commit accompanying documentation under `docs/sagas/` with sample code and contract tests.
- Add the `schemaCompatibilityCheck` Gradle task and wire it into CI pipelines alongside ktlint/detekt and the future SpotBugs/ErrorProne gates.
- Finish API gateway, Debezium connector, and polyglot datastore runbooks referenced in Operational Automation; link them from `docs/runbooks/`.
- Implement OpenTelemetry tracing across all services and create observability dashboards
- Implement centralized logging with ELK stack
- Complete OpenTelemetry implementation with collector and Jaeger backend

## Documentation Hygiene

- Keep `@IMPLEMENTATION_PLAN.md` synchronized with roadmap changes; link additional subsystem guides using `@docs/<name>.md`.
- Record schema evolution decisions in `docs/schemas/CHANGELOG.md`; require backward-compatible changes unless otherwise approved.
- Maintain `docs/architecture/service-catalog.md`, `docs/notes/phase-0-*.md`, and ADRs in `docs/adrs/` as the source of truth for discovery outcomes.
- Ensure `docs/dev/getting-started.md` stays current with tooling and workflow changes.
- Keep runbooks up-to-date with implementation changes and regularly review for accuracy

## Transactional Outbox Pattern Implementation

### Overview

The project implements the transactional outbox pattern using both Debezium as the primary mechanism and a polling relay as a fallback. This approach ensures that domain data changes and event publications happen atomically within the same database transaction, eliminating the risk of inconsistency between the database and the message broker.

### Implementation Approaches

The implementation provides two mechanisms for event publication:

1. **Primary: Debezium CDC with Outbox Event Router SMT**
    - Uses Debezium to capture changes from the outbox table
    - Routes events to appropriate Kafka topics based on aggregate type
    - Provides low-latency, exactly-once delivery semantics

2. **Fallback: Polling Relay**
    - Scheduled service that polls the outbox table for pending messages
    - Publishes events to Kafka using transactional producers
    - REST endpoints for manual processing and replay

### Components

#### 1. Outbox Entity Structure

The `OutboxMessage` entity in `common-outbox-relay` defines the structure for storing events:

```kotlin
@Entity
@Table(name = "outbox")
open class OutboxMessage(
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "headers", columnDefinition = "TEXT")
    val headers: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set
}

enum class OutboxStatus {
    PENDING,
    SENT,
    FAILED,
}
```

#### 2. Outbox Table Schema

Each service's database contains an `outbox` table defined in Flyway migrations:

```sql
CREATE TABLE IF NOT EXISTS public.outbox (
                                             id UUID PRIMARY KEY,
                                             aggregate_type TEXT NOT NULL,
                                             aggregate_id TEXT NOT NULL,
                                             event_type TEXT NOT NULL,
                                             payload TEXT NOT NULL,
                                             headers TEXT NULL,
                                             status TEXT NOT NULL DEFAULT 'PENDING',
                                             occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                             published_at TIMESTAMP WITH TIME ZONE NULL,
                                             version BIGINT NOT NULL DEFAULT 0
);
```

#### 3. How Services Use the Outbox Pattern

In the `orders-service`, when handling a `CreateOrderCommand`:

```kotlin
@Transactional
fun handle(command: CreateOrderCommand): UUID {
    validate(command)
    val occurredAt = Instant.now()

    // Save domain entity
    val order = OrderEntity(/* ... */)
    val saved = orderRepository.save(order)

    // Create outbox record
    val event = OrderCreatedEvent.newBuilder()
        .setEventId(UUID.randomUUID())
        .setAggregateId(saved.id!!)
        .setOccurredAt(occurredAt)
        .setPayload(serializePayload(saved.id!!, command))
        .build()

    val payload = encodeEvent(event)

    val outbox = OutboxMessage(
        aggregateId = saved.id!!.toString(),
        aggregateType = "Order",
        eventType = "OrderCreated",
        payload = payload,
        headers = null,
        status = OutboxStatus.PENDING,
        occurredAt = occurredAt,
    )
    outboxRepository.save(outbox)

    // Rest of the method...
    return saved.id!!
}
```

This ensures that both the domain data and the event are persisted atomically within the same transaction.

### Debezium Implementation (Primary Approach)

The Debezium connectors are configured with the Outbox Event Router SMT to process outbox table changes:

```json
{
    "name": "orders-outbox-connector",
    "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "tasks.max": "1",
        "database.hostname": "postgres",
        "database.port": "5432",
        "database.user": "app",
        "database.password": "app",
        "database.dbname": "orders",
        "topic.prefix": "orders",
        "table.include.list": "public.outbox",
        "plugin.name": "pgoutput",
        "publication.name": "outbox_publication",
        "publication.autocreate.mode": "filtered",
        "slot.name": "orders_outbox_slot",
        "snapshot.mode": "no_data",
        "transforms": "outbox",
        "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
        "transforms.outbox.table.field.event.key": "aggregate_id",
        "transforms.outbox.table.fields.additional.placement": "id:envelope:eventId,aggregate_id:envelope:aggregateId,aggregate_type:envelope:aggregateType,event_type:envelope:eventType,headers:envelope:headers",
        "transforms.outbox.route.topic.replacement": "outbox.${routedByValue}",
        "transforms.outbox.route.by.field": "aggregate_type",
        "transforms.outbox.operation.routing.enabled": "false",
        "key.converter": "org.apache.kafka.connect.storage.StringConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable": "false",
        "internal.key.converter": "org.apache.kafka.connect.json.JsonConverter",
        "internal.value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "internal.key.converter.schemas.enable": "false",
        "internal.value.converter.schemas.enable": "false",
        "topic.creation.default.partitions": "1",
        "topic.creation.default.replication.factor": "1"
    }
}
```

When an outbox record is inserted into the database:

1. Debezium captures the change through PostgreSQL's logical replication
2. The Outbox Event Router transforms the change into a Kafka message
3. The message is routed to a topic based on the `aggregate_type` (e.g., "Order" → "outbox.Order")
4. The message is published to Kafka with the `aggregate_id` as the key

### Polling Relay Implementation (Fallback Approach)

The polling relay is implemented in the `common-outbox-relay` module and provides both scheduled and on-demand processing:

#### Scheduled Processing

The `ScheduledOutboxProcessor` runs every 5 seconds to check for pending messages:

```kotlin
@Component
class ScheduledOutboxProcessor(
    private val outboxRelayService: OutboxRelayService,
    meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(ScheduledOutboxProcessor::class.java)
    private val pendingMessagesGauge = AtomicLong(0)

    init {
        Gauge
            .builder("outbox.pending.messages", pendingMessagesGauge) { it.toDouble() }
            .description("Number of pending outbox messages awaiting processing")
            .register(meterRegistry)
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    fun processPendingMessages() {
        try {
            logger.debug("Scheduled outbox processing starting...")
            val processedCount = outboxRelayService.processPendingMessages()
            logger.debug("Scheduled outbox processing completed. Processed $processedCount messages")

            // Update the gauge with current pending count
            val pendingCount = outboxRelayService.getPendingMessageCount()
            pendingMessagesGauge.set(pendingCount)
        } catch (e: Exception) {
            logger.error("Error during scheduled outbox processing", e)
        }
    }
}
```

#### Outbox Relay Service

The `OutboxRelayService` processes messages in batches:

```kotlin
@Service
class OutboxRelayService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val transactionTemplate: TransactionTemplate,
    private val kafkaTransactionManager: KafkaTransactionManager<String, Any>,
    private val metricsService: OutboxMetricsService,
) {
    private val logger = LoggerFactory.getLogger(OutboxRelayService::class.java)
    private val batchSize = 100

    @Transactional
    fun processPendingMessages(): Int {
        logger.info("Starting outbox relay processing")

        var processedCount = 0
        var hasMoreMessages = true

        while (hasMoreMessages) {
            val pendingMessages =
                outboxRepository.findByStatusOrderByOccurredAtAsc(
                    OutboxStatus.PENDING,
                    PageRequest.of(0, batchSize),
                )

            if (pendingMessages.isEmpty()) {
                hasMoreMessages = false
                continue
            }

            logger.info("Processing batch of ${pendingMessages.size} pending outbox messages")

            pendingMessages.forEach { message ->
                try {
                    val startTime = System.currentTimeMillis()
                    processMessage(message)
                    val endTime = System.currentTimeMillis()

                    metricsService.recordProcessedMessage()
                    metricsService.recordEndToEndLatency(endTime - startTime)
                    processedCount++
                } catch (e: Exception) {
                    logger.error("Failed to process outbox message with id: ${message.id}", e)
                    markMessageAsFailed(message)
                    metricsService.recordFailedMessage()
                }
            }

            // If we got less than batch size, there are no more messages
            if (pendingMessages.size < batchSize) {
                hasMoreMessages = false
            }
        }

        // Update pending count metric
        updatePendingMessageCountMetric()

        logger.info("Finished outbox relay processing. Processed $processedCount messages")
        return processedCount
    }

    private fun processMessage(message: OutboxMessage) {
        try {
            // Determine the topic based on the aggregate type or event type
            val topic = determineTopic(message)

            // Extract key from the message if available, otherwise use aggregate ID
            val key = extractKey(message) ?: message.aggregateId

            // Convert payload to the appropriate object or keep as string
            val payload = convertPayload(message)

            // Send message within Kafka transaction
            val sendStartTime = System.currentTimeMillis()
            kafkaTemplate.executeInTransaction { operations ->
                operations.send(topic, key, payload)
                logger.debug("Sent message to topic $topic with key $key")
                true
            }
            val sendEndTime = System.currentTimeMillis()

            metricsService.recordRelayLatency(sendEndTime - sendStartTime)

            // Mark message as sent in the database
            markMessageAsSent(message)
        } catch (e: Exception) {
            logger.error("Failed to process outbox message with id: ${message.id}", e)
            markMessageAsFailed(message)
            metricsService.recordFailedMessage()
            throw e
        }
    }

    // Other methods...
}
```

#### REST API Endpoints

The relay also provides REST endpoints for manual processing:

```kotlin
@RestController
@RequestMapping("/api/outbox")
class OutboxController(
    private val outboxRelayService: OutboxRelayService,
) {
    @PostMapping("/process")
    fun processPendingMessages(): ResponseEntity<Map<String, Any>> {
        val processedCount = outboxRelayService.processPendingMessages()
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to processedCount,
                "message" to "Processed $processedCount pending messages",
            ),
        )
    }

    @PostMapping("/replay/{messageId}")
    fun replayMessage(
        @PathVariable messageId: String,
    ): ResponseEntity<Map<String, Any>> {
        val success = outboxRelayService.replayMessage(messageId)
        return if (success) {
            ResponseEntity.ok(
                mapOf(
                    "message" to "Successfully replayed message $messageId",
                ),
            )
        } else {
            ResponseEntity.badRequest().body(
                mapOf(
                    "error" to "Failed to replay message $messageId",
                ),
            )
        }
    }

    // Other endpoints...
}
```

### Kafka Configuration

The Kafka configuration in `common-kafka` ensures proper transactional behavior:

```kotlin
@Configuration
class KafkaProducerConfig {
    companion object {
        private const val MAX_IN_FLIGHT_REQUESTS = 5
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory::class)
    fun producerFactory(kafkaProperties: KafkaProperties): ProducerFactory<String, Any> {
        val props = kafkaProperties.buildProducerProperties()
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.RETRIES_CONFIG] = Integer.MAX_VALUE
        props[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = MAX_IN_FLIGHT_REQUESTS

        val transactionIdPrefix =
            kafkaProperties.producer.transactionIdPrefix?.takeIf { it.isNotBlank() } ?: "payments-tx-"

        return DefaultKafkaProducerFactory<String, Any>(props).apply {
            setTransactionIdPrefix(transactionIdPrefix)
        }
    }

    // Other beans...
}
```

### Key Features

1. **Exactly-Once Semantics**: Achieved through Kafka transactions and Debezium's Outbox Event Router
2. **High Availability**: The polling relay serves as a fallback when Debezium is unavailable
3. **Monitoring**: Metrics collection for pending messages, processed messages, and failures
4. **Replay Capability**: Ability to replay specific messages for recovery scenarios
5. **Idempotency**: Consumers can check a processed events table to avoid duplicate processing
6. **Flexible Routing**: Events are routed based on aggregate type to appropriate Kafka topics

### Outbox Pattern Best Practices

1. **Event Design**: Events should be designed to be immutable and backward compatible
2. **Idempotency**: Consumers should be designed to handle duplicate events idempotently by checking a processed events table
3. **Monitoring**: Monitor outbox table depth and Debezium connector lag to ensure healthy operation
4. **Error Handling**: Implement proper error handling and dead letter queues for event processing failures
5. **Configuration**: Enable the polling relay with `outbox.relay.enabled=true` when Debezium is not available
