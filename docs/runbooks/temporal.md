# Temporal Worker Runbook (Draft)

## Scope
Deployment and monitoring guidelines for Temporal workflows used in cross-domain sagas. Extends research captured in `docs/research/temporal-options.md`.

## Setup
1. Provision Temporal Cloud namespace (Phase 4 pilot).
2. Deploy worker pods per bounded context (`orders-worker`, `payments-worker`).
3. Configure worker credentials via Vault and mount as Kubernetes secrets.

## Worker Deployment

Temporal workers are configured and launched via the `temporal-spring-boot-starter-kotlin`.
Workers are automatically discovered and started by annotating workflow and activity implementations with `@Component`.

- **Configuration**: Worker task queues and other settings are managed in `application.yml` under the `temporal.*` namespace.
- **Deployment**: Workers are deployed as part of their respective Spring Boot microservices (e.g., `orders-service`).

## Operations
- Monitor worker heartbeats (Temporal Cloud console) and exported metrics (`temporal_worker_task_queue_poll_succeeded_total`).
- Set retry policies in workflows and activities to align with saga compensation logic.
- Document activity idempotency requirements and align with outbox event schemas.

## Incident Response
- Temporarily pause workflows via Temporal CLI if downstream systems are degraded.
- Replay workflow history to reproduce issues (`temporal workflow show --workflow-id <id>`).

## References
- Temporal Cloud onboarding docs (2025-09 release)
- Temporal OSS production checklist
