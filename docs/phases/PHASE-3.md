# Phase 3 – Outbox Relay & Tooling

## Objectives

- Validate Debezium CDC vs polling relay and finalize default selection.
- Build tooling for replaying outbox events and monitoring connector health.
- Integrate schema compatibility checks into CI pipelines.

## Deliverables

- Comparative spike report on Debezium vs poller with decision recorded.
- Replay tooling script (`scripts/outbox-replay.sh`) with usage guide.
- CI job executing `./gradlew schemaCompatibilityCheck` with failure remediation steps.
- Monitoring dashboards for Debezium lag and connector status.

## Task Board

| ID      | Task                                                                                           | Owner             | Status      | Notes                                                                                                                                                                              |
|---------|------------------------------------------------------------------------------------------------|-------------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P3.1    | Execute spike comparing Debezium vs polling relay performance                                  | Architecture Team | Completed   | Findings recorded in `docs/research/outbox-relay.md`.                                                                                                                              |
| P3.2    | Update ADR with final outbox publisher decision                                                | Architecture Team | Completed   | ADR 0001 updated with BinaryDataConverter + schema export workflow.                                                                                                                |
| P3.3    | Implement `scripts/outbox-replay.sh` and document usage                                        | Platform Team     | Completed   | Script added in `scripts/outbox-replay.sh`; runbook updated.                                                                                                                       |
| P3.4    | Wire `schemaCompatibilityCheck` into CI                                                        | Platform Team     | Completed   | Gradle task + CI workflow (`schema-compatibility.yml`) ensure schema validation on PRs. Additional security scanning with SpotBugs and ErrorProne gates added to main CI workflow. |
| P3.5    | Build Grafana dashboard panels for Debezium lag/health                                         | Platform Team     | Completed   | Grafana dashboard panels for Debezium lag and event age are defined; configuration is tracked alongside observability runbooks.                                                    |
| P3.6    | Document connector deployment runbook updates                                                  | Platform Team     | Completed   | Added `docs/runbooks/debezium.md` with deployment/replay guidance.                                                                                                                 |
| P3.7    | Evaluate Temporal deployment options (self-hosted vs cloud)                                    | Architecture Team | Completed   | Findings in `docs/research/temporal-options.md`; follow-up runbook planned for Phase 4.                                                                                            |
| P3.8    | Assess CDN/edge caching provider integration                                                   | Architecture Team | Completed   | Evaluation captured in `docs/research/cdn-evaluation.md`.                                                                                                                          |
| P3.9    | Create shared Avro schema module and publishing pipeline                                       | Platform Team     | Completed   | Module scaffolded; `exportAvroSchemas` and `scripts/schema-publish.sh` provide export/publish steps.                                                                               |
| P3.10   | Configure Debezium connectors for Avro outbox payloads                                         | Platform Team     | Completed   | Connector JSON uses BinaryDataConverter; bootstrap steps documented in Debezium + schema registry runbooks.                                                                        |
| P3.DB11 | Normalize Debezium connectors (publication.autocreate.mode, slot.name, event.key=aggregate_id) | Platform Team     | In Progress | Update JSON under `infra/debezium/connectors/`.                                                                                                                                    |
| P3.DB12 | Retire legacy `infra/debezium/outbox-connector.json` (camelCase/BinaryDataConverter)           | Platform Team     | Planned     | Avoid schema drift/confusion.                                                                                                                                                      |
| P3.DB13 | Canonicalize outbox schema + CDC vs poller rules                                               | Architecture Team | Planned     | CDC is default; isolate poller to non-CDC services.                                                                                                                                |

## Research & References

- Debezium lag monitoring best practices
- Schema Registry compatibility docs
- Kafka Connect operations guides

## Risks & Mitigations

- **Connector instability**: Plan automated restarts and alerting.
- **Replay misuse**: Require dry-run mode and audit logging.

## Dependencies

- Phase 1 infrastructure and Phase 2 shared modules.
- Access to monitoring stack (Prometheus/Grafana).

## Artifacts & Links

- Spike report (`docs/research/outbox-relay.md`)
- Replay script (`scripts/outbox-replay.sh`)
- CI config updates (`.github/workflows/`)
- Debezium runbook (`docs/runbooks/debezium.md`)
- Polling relay ADR (`docs/adrs/0005-polling-relay-mechanism.md`)

## Progress Log

- 2025-10-08 | Added Avro schema + connector configuration tasks; scaffolding complete (`common-events-avro`, `exportAvroSchemas`, `scripts/schema-publish.sh`) with CI validation (P3.4/P3.9 Completed). Connector JSON uses `BinaryDataConverter` and runbooks document bootstrap steps (P3.10 Completed). Debezium runbook & replay script delivered (P3.3/P3.6 Completed). Research docs added for outbox relay, Temporal, CDN (P3.1/P3.7/P3.8 Completed). Grafana dashboard now contains lag/event age thresholds (P3.5 Completed).
- 2025-10-10 | Implemented polling relay mechanism as fallback to Debezium with scheduled processing, REST API endpoints, metrics collection, and health indicators (P3.11 Completed). Documented in ADR 0005 and updated Debezium runbook with polling relay information.
