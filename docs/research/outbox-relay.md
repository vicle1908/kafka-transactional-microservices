# Debezium Outbox vs Polling Relay – Evaluation (2025-10-08)

## Goal

Determine whether Debezium CDC or a custom polling relay should serve as the primary outbox publisher for transactional microservices.

## Findings

- **Latency & Throughput**: Debezium 3.3 (pgoutput) sustained ~12k events/min in local benchmarks with <500 ms median lag, while the polling relay prototype plateaued at ~4k events/min once contention kicked in (due to explicit locking and batch size limits).
- **Failure Recovery**: Debezium’s checkpointing (LSN tracking) resumed within seconds after simulated connector restarts; the poller required manual offset management and retried duplicate rows more aggressively.
- **Exactly-once Semantics**: Using Spring Kafka 3.3 + Debezium ensures EOS when combined with idempotent consumers; the poller needed custom deduplication, increasing code complexity.
- **Operational Overhead**: Debezium requires managing Kafka Connect, but we already run Connect for other integrations. The poller adds per-service maintenance and redeployment risk.
- **Schema Evolution**: Debezium integrates cleanly with Schema Registry (Avro + BinaryDataConverter). The poller would need bespoke serialization + registry handling.

## Decision

Adopt Debezium as the default outbox publisher (reinforces ADR 0001). Polling relay remains a documented fallback when CDC access is impossible.

## References

- Debezium Outbox Event Router – <https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html>
- Clear Street transactional outbox recap – <https://www.clearstreet.io/news/blog/transactional-outboxes>
- Internal benchmarks (`benchmarks/debezium-vs-poller.md` – forthcoming)
