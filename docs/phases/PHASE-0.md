# Phase 0 – Discovery & Architecture

## Objectives

- Align on microservice scope, data stores, and consistency requirements.
- Ratify the baseline tech stack and architectural decisions.
- Produce ADRs for transactional outbox, saga choreography, and schema governance.

## Deliverables

- Stakeholder-approved service catalog and data flow maps.
- Architecture decision records (outbox, saga style, schema policy).
- Updated @AGENTS.md and IMPLEMENTATION_PLAN.md reflecting discovery outcomes.

## Task Board

| ID   | Task                                                          | Owner             | Status    | Notes                                                                |
|------|---------------------------------------------------------------|-------------------|-----------|----------------------------------------------------------------------|
| P0.1 | Schedule discovery workshops with domain stakeholders         | Architecture Team | Completed | See `docs/notes/phase-0-schedule.md`.                                |
| P0.2 | Inventory microservice data stores and consistency needs      | Architecture Team | Completed | Documented in `docs/architecture/service-catalog.md`.                |
| P0.3 | Confirm tech stack (Spring Boot LTS, Kafka LTS, Debezium, DB) | Platform Team     | Completed | See `docs/notes/phase-0-tech-stack.md` and `docs/version-matrix.md`. |
| P0.4 | Draft ADR: transactional outbox strategy (Debezium default)   | Architecture Team | Completed | Recorded in `docs/adrs/0001-transactional-outbox.md`.                |
| P0.5 | Draft ADR: saga choreography vs orchestration                 | Architecture Team | Completed | See `docs/adrs/0002-saga-coordination.md`.                           |
| P0.6 | Draft ADR: schema governance & compatibility policy           | Architecture Team | Completed | See `docs/adrs/0003-schema-governance.md`.                           |
| P0.7 | Update @AGENTS.md and IMPLEMENTATION_PLAN.md with decisions   | Architecture Team | Completed | Documentation updated with discovery outcomes.                       |
| P0.8 | Draft ADR: secrets management strategy                        | Architecture Team | Completed | See `docs/adrs/0004-secrets-management.md`.                          |

## Research & References

- Debezium outbox SMT documentation
- Spring Kafka EOS configuration guides
- Domain context docs from stakeholders

## Risks & Mitigations

- **Unclear ownership**: Assign decision owners during workshops.
- **Scope creep**: Time-box discovery and log deferred items in backlog.

## Dependencies

- Availability of product owners and lead engineers.
- Access to existing system diagrams and data lineage docs.

## Artifacts & Links

- Meeting notes (`docs/notes/phase-0/*.md`)
- ADRs (`docs/adrs/`)
- Service catalog spreadsheet or diagram

## Progress Log

- 2025-10-07 | Workshops scheduled, service catalog drafted, ADRs 0001–0004 approved, docs updated.
