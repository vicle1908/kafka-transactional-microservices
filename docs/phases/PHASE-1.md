# Phase 1 – Platform Foundation

## Objectives
- Provision shared Kafka/Postgres infrastructure for dev and CI environments.
- Harden brokers for transactional workloads with required settings.
- Establish CI/CD scaffolding and IaC pipelines.

## Deliverables
- Docker Compose stack (`infra/compose.yml`) verified locally.
- Shared Kafka cluster with Schema Registry, AKHQ/Kafdrop access.
- CI pipelines covering build, test, container publish, IaC validation.

## Task Board
|| ID | Task | Owner | Status | Notes |
||----|------|-------|--------|-------|
|| P1.1 | Stand up local Docker Compose stack (Kafka, Postgres, Debezium, Schema Registry) | Platform Team | Completed | `infra/compose.yml` ready for local use. |
|| P1.2 | Provision shared Kafka cluster with pure KRaft mode (no ZooKeeper) and configure ACLs | Platform Team | Completed (Plan) | See `docs/notes/phase-1-shared-cluster.md`. |
|| P1.3 | Enable broker settings (min.insync.replicas, idempotence defaults, transaction log config) | Platform Team | Completed | Config captured in `infra/kafka/values.yaml`. |
|| P1.4 | Deploy AKHQ or Kafdrop for topic inspection | Platform Team | Completed | AKHQ defined in compose stack. |
|| P1.5 | Create GitHub Actions/GitLab pipelines for build/test and image publish | DevOps Team | Completed | `.github/workflows/ci.yml` and `.github/workflows/container-publish.yml` committed. |
|| P1.6 | Add IaC validation job (Terraform/Helm lint) to pipeline | DevOps Team | Completed | `ci/scripts/validate-iac.sh` integrated. |
|| P1.7 | Install Istio ambient profile in non-prod cluster | Platform Team | Completed (Plan) | Deployment steps documented in `infra/istio/README.md`. |
|| P1.8 | Update runbooks for infra bootstrap and service mesh | Platform Team | Completed | `docs/runbooks/platform.md` and `docs/runbooks/service-mesh.md` updated. |
|| P1.DB1 | Configure Postgres for logical replication (wal_level=logical, max_wal_senders, max_replication_slots) | Platform Team | Planned | Use `command:` flags in compose. |
|| P1.DB2 | Create multiple databases (payments, inventory, notifications) + replication role | Platform Team | Planned | Add `infra/postgres/init/01-create-dbs.sql`; create `debezium` user. |
|| P1.DB3 | Document DB bootstrap & Debezium prerequisites | Platform Team | Planned | Update `docs/runbooks/debezium.md`. |

|| P1.9 | Implement comprehensive health checks for all services in Docker Compose files | Platform Team | Completed | Added health checks to all services in `infra/compose.yml`, `infra/compose.prod.yml`, and `infra/compose.test.yml`. |

|| P1.ENV1 | Add root `.env.example` (app/runtime variables, dev‑safe defaults) | Developer Experience | Planned | Keep `.env` git‑ignored per `.gitignore`. |
|| P1.ENV2 | Add `infra/.env.example` (Compose variables: ports, images) | Developer Experience | Planned | Enable `docker compose --env-file infra/.env` usage. |
|| P1.ENV3 | Add `.envrc` (direnv) + `scripts/export-env.sh` fallback | Developer Experience | Planned | Auto‑load env locally without committing secrets. |
|| P1.ENV4 | Update service `application.yml` to resolve from env with fallbacks | Service Owners | Planned | DB URL/user/pass, Kafka bootstrap servers, Schema Registry URL, Redis host/port, OTEL toggles, tx prefixes; do not change `application-test.yml`. |
|| P1.ENV5 | Optional: Parameterize `infra/compose.yml` ports/images via `${VAR:-default}` | Platform Team | Planned | Respect `infra/.env` without secrets; keep behavior identical by default. |
|| P1.ENV6 | Update `docs/dev/getting-started.md` with ".env workflow" | Developer Experience | Planned | Include direnv/export script instructions; cross‑link ADR‑0004 and Vault runbook. |
|| P1.ENV7 | Acceptance: validate services boot with env defaults; CI unaffected | Platform + Service Owners | Planned | No secrets in VCS; compose respects `infra/.env`; workflows continue to work. |

## Research & References
- Kafka 4.1 KRaft deployment guides
- Schema Registry and AKHQ hardening best practices
- CI templates for Gradle + Docker

## Risks & Mitigations
- **Networking restrictions**: Coordinate with ops for firewall rules.
- **Secrets management**: Integrate Vault/AWS Secrets Manager early.

## Dependencies
- Ops team for infrastructure provisioning rights.
- Access to container registry and secret stores.

## Artifacts & Links
- `infra/compose.yml`
- CI pipeline configs (`.github/workflows/` or `.gitlab-ci.yml`)
- Platform runbook (`docs/runbooks/platform.md`)

## Progress Log
- 2025-10-07 | Compose stack, CI pipelines, Istio deployment notes, and runbooks prepared.
- 2025-10-10 | Implemented comprehensive health checks for all services in Docker Compose files (development, test, and production environments).
