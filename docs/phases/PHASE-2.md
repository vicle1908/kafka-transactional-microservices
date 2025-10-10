# Phase 2 – Service Template & Shared Components

## Objectives

- Establish the multi-module Gradle structure and shared libraries.
- Implement transactional outbox schema and persistence wiring.
- Introduce shared tooling for version enforcement and developer onboarding.

## Deliverables

- Modules: `common-events`, `common-kafka`, `common-persistence` scaffolded.
- Flyway migrations for outbox tables with validation tests.
- Gradle `versionCheck` task and `docs/version-matrix.md` baseline.
- Developer onboarding guide (`docs/dev/getting-started.md`).

## Task Board

| ID | Task | Owner | Status | Notes |
|----|------|-------|--------|-------|
| P2.1 | Create Gradle multi-module skeleton with shared convention plugins | Platform Team | Completed | `settings.gradle.kts`, root build, and version catalog configured for Kotlin multi-module build including `common-observability` for OpenTelemetry tracing; generate Gradle wrapper (e.g., copy from `/Users/vinhlekhanh/Downloads/project/company/times/githubusers`). |
| P2.2 | Implement outbox JPA entities, repositories, and Flyway migrations | Persistence Team | Completed | Kotlin entity + Flyway migration with tests in `common-persistence`. |
| P2.3 | Wire Spring Kafka transaction manager and template defaults | Messaging Team | Completed | `common-kafka` Kotlin config centralizes producer settings. |
| P2.4 | Package Debezium connector templates (`infra/debezium/*.json`) | Platform Team | Completed | Template stored in `infra/debezium/outbox-connector.json`. |
| P2.5 | Author `docs/version-matrix.md` with initial latest-stable entries | Architecture Team | Completed | Matrix populated with Kotlin, Spring Boot, Kafka, Debezium versions. |
| P2.6 | Implement Gradle `versionCheck` task and enforce in CI | DevOps Team | Completed | Task lives in root build; CI workflow invokes it. |
| P2.7 | Write developer onboarding guide covering setup, tests, linting | Developer Experience | Completed | `docs/dev/getting-started.md` created. |
| P2.8 | Update @AGENTS.md with template outcomes | Architecture Team | Completed | Baseline updated with Kotlin-first approach and doc hygiene guidance. |
| P2.9 | Design Redis caching layer blueprint (cache-aside policies, invalidation) | Architecture Team | Completed | See `docs/architecture/caching.md`. |
| P2.10 | Implement ktlint/detekt/jacoco quality gates and `.editorconfig` | Developer Experience | Completed | Gradle `check` runs ktlint + detekt + Jacoco; config lives under `config/detekt/` and root `.editorconfig`; configuration cache + parallel execution enabled in `gradle.properties`. |
| P2.11 | Create `common-temporal` module for shared workflow definitions | Platform Team | Completed | To house shared workflow interfaces, activities, and DTOs. |

## Research & References

- Gradle multi-module best practices (convention plugins)
- Spring transactional outbox examples
- Debezium connector configuration guides

## Risks & Mitigations

- **Config duplication**: Use shared Gradle scripts to avoid drift.
- **Schema drift**: Automate migration tests and review process.

## Dependencies

- Phase 1 infrastructure ready for integration tests.
- Access to schema registry for event schema publishing.

## Artifacts & Links

- `build.gradle.kts` modules
- `docs/version-matrix.md`
- `docs/dev/getting-started.md`

## Progress Log

- 2025-10-07 | Kotlin multi-module foundation, shared libraries, Debezium template, version catalog, and onboarding docs completed.
