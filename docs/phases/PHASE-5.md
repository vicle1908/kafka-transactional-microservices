# Phase 5 – Observability & Resilience

## Objectives
- Instrument services for tracing, metrics, and logging aligned with platform standards.
- Implement resilience patterns (retries, DLQs, chaos drills) and document runbooks.
- Finalize operational automation for API gateway, Debezium connectors, and polyglot datastores.

## Deliverables
- OpenTelemetry tracing configured end to end (HTTP + Kafka).
- Grafana dashboards and alert rules for transactions, lag, DLQs, saga failures.
- Completed runbooks: API gateway, Debezium connectors, polyglot datastore adapters.

## Task Board
| ID | Task | Owner | Status | Notes |
|----|------|-------|--------|-------|
| P5.1 | Integrate OpenTelemetry SDK and propagate context headers | Platform Team | Completed | Implemented in `common-observability` module with configuration classes and Kafka tracing utilities. |
| P5.2 | Configure Micrometer metrics exporters and dashboards | Platform Team | Completed | Implemented in `common-observability` module with Prometheus integration and Kafka client metrics. |
| P5.3 | Implement retry/DLQ policies and chaos drills | DevOps Team | Completed | GitHub Actions workflow (`chaos-engineering.yml`) implementing broker restart, DB failover, mesh failure, and cache outage drills with monitoring and reporting. Document drill outcomes. |
| P5.4 | Configure Temporal Micrometer metrics and OTel tracing | TBD | Not Started | Export metrics to Prometheus and add tracing interceptor to workers. |
| P5.5 | Create Grafana dashboard for Temporal metrics | TBD | Not Started | Visualize workflow latency, activity failures, and retry rates. |
| P5.6 | Author API gateway runbook (`docs/runbooks/api-gateway.md`) | TBD | Not Started | Cover routing, auth, rollout, rollback. |
| P5.7 | Author Istio service mesh runbook (`docs/runbooks/service-mesh.md`) | TBD | Not Started | Include ambient mode rollout, traffic policy, troubleshooting. |
| P5.8 | Author Debezium connector runbook (`docs/runbooks/debezium.md`) | TBD | Not Started | Include deployment automation and troubleshooting. |
| P5.9 | Author polyglot datastore runbook (`docs/runbooks/polyglot-datastore.md`) | TBD | Not Started | Capture adapter-specific monitoring. |
| P5.10 | Instrument CDN/edge metrics and dashboards | TBD | Not Started | Monitor cache hit ratio, latency, error rates. |
| P5.11 | Automate runbook validation checks in CI (link checker, lint) | TBD | Not Started | Use markdown linting scripts. |

## Research & References
- OpenTelemetry instrumentation guides for Spring
- Kafka resilience patterns and chaos engineering playbooks
- Gateway operation best practices (Spring Cloud Gateway)

## Risks & Mitigations
- **Monitoring gaps**: Run observability reviews with SRE/Ops.
- **Runbook rot**: Schedule periodic audits via CI lint jobs.

## Dependencies
- Service implementations from Phase 4.
- Monitoring stack availability.

## Artifacts & Links
- Observability configs (`config/observability/*`)
- Runbooks (`docs/runbooks/*.md`)
- Chaos drill reports (`docs/chaos/`)

## Progress Log
- YYYY-MM-DD | Initial placeholders added.
