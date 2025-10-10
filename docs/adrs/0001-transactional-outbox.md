# ADR 0001 – Transactional Outbox Strategy

## Status
Accepted – 2025-10-07

## Context
We must ensure domain database writes and Kafka event publications occur atomically. Two options considered:
1. Debezium CDC outbox pattern.
2. Application-level polling relay without CDC.

## Decision
Adopt Debezium 3.3.0.Final outbox SMT as the default publisher. Each service writes to an `outbox` table within the same transaction as domain updates. Debezium captures rows and publishes to Kafka topics with exactly-once semantics. Polling relay is reserved for environments where CDC privileges are unavailable; ADR notes the fallback criteria and configuration.

## Consequences
- Guarantees atomicity without 2PC; Debezium handles delivery and ordering.
- Requires managing Debezium connectors and monitoring lag.
- Fallback poller must implement shared retry/backoff and idempotency rules and is documented as an exception path.
- Connection profile: connectors emit Avro payloads via `BinaryDataConverter`; schemas are published ahead of time using `./gradlew exportAvroSchemas` and validated in CI to ensure compatibility.
