# Temporal Deployment Options (2025-10-08)

## Options Evaluated
1. **Temporal Cloud** – managed control plane, workers run in our cluster. Worthwhile for rapid adoption and reduced ops.
2. **Self-hosted Temporal OSS** – full control over persistence (Cassandra/Postgres + Elasticsearch). Higher ops burden.
3. **Alternative orchestrators (Camunda 8, Zeebe)** – not aligned with existing Kotlin/Spring expertise; weaker Spring Kafka integration.

## Criteria & Scoring
| Criterion | Temporal Cloud | Temporal OSS | Camunda 8 |
| --- | --- | --- | --- |
| Time-to-value | ✅ | ⚠️ | ⚠️ |
| Operational overhead | ✅ | ❌ | ⚠️ |
| Feature completeness (signals, async retries) | ✅ | ✅ | ⚠️ |
| Cost predictability | ⚠️ (usage-based) | ✅ | ⚠️ |
| Ecosystem fit (Kotlin, gRPC) | ✅ | ✅ | ⚠️ |

## Recommendation
Adopt Temporal Cloud for pilot sagas in Phase 4, revisit self-hosting once traffic and tenancy requirements stabilise. Document runbook for worker deployments in `docs/runbooks/temporal.md` (future work).

## References
- Temporal Cloud pricing & SLA (Sep 2025)
- Temporal OSS production checklist (https://docs.temporal.io/self-host-production)
- Camunda 8 migration guide (reviewed for completeness)
