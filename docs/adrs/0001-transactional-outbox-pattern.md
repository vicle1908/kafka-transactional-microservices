# ADR 0001: Transactional Outbox Pattern for Exactly-Once Semantics

## Status
Proposed

## Context
In a microservices architecture with event-driven communication, we need to ensure that when a business transaction occurs in a service, both the state change is persisted in the service's database AND the corresponding event is published to Kafka. The "dual write" problem occurs when these two operations are not atomic, potentially leading to:
- Event published without state change (data inconsistency)
- State change without event publication (lost updates)
- Duplicate events (if retrying failed publications)

Traditional distributed transactions (2PC) are not suitable for microservices due to their blocking nature and operational complexity.

## Decision
We will implement the Transactional Outbox Pattern combined with Kafka's Exactly-Once Semantics (EOS) to guarantee atomicity between database writes and message publication.

## Approach
1. **Atomic Database Write**: When a business operation occurs, the service writes both the business data and the event to be published into an `outbox` table within the same database transaction.
2. **Reliable Message Relay**: A separate process (using Debezium CDC or a custom poller) reads from the `outbox` table and reliably publishes messages to Kafka using Kafka's transactional producer API.
3. **Consumer Isolation**: Kafka consumers are configured to read only committed transactions to ensure they see consistent, non-duplicate messages.

## Configuration Requirements
### Database Schema
- Outbox table with `id`, `aggregate_id`, `aggregate_type`, `event_type`, `payload`, `status`, `created_at`, `published_at` columns
- Status field tracks `PENDING`, `SENT`, `FAILED` states
- Proper indexing for efficient polling and querying

### Kafka Broker Configuration
- `transaction.state.log.replication.factor >= 3` (production) or 1 (development)
- `transaction.state.log.min.isr >= 2` (production) or 1 (development)
- Idempotence and transaction support enabled

### Kafka Producer Configuration
- `enable.idempotence = true`
- `acks = all`
- `transactional.id` unique per producer instance

### Kafka Consumer Configuration
- `isolation.level = read_committed`
- `enable.auto.commit = false` (for manual offset management)

## Alternatives Considered
### At-Least-Once with Idempotent Consumers
- Simpler to implement but requires consumers to be idempotent
- May result in duplicate processing, requiring additional deduplication logic
- Chosen for services where exactly-once is not critical

### Distributed Transactions (2PC)
- Would solve the atomicity problem but introduces blocking, complexity, and operational risks
- Anti-pattern in microservices architecture

## Consequences
### Positive
- Strong consistency guarantee between business state and event publication
- Eliminates the dual-write problem
- Enables reliable event-driven architectures
- Good operational characteristics compared to distributed transactions

### Negative
- Added operational complexity with the outbox relay service
- Additional infrastructure component requiring monitoring and maintenance
- Potential performance overhead from transactional processing
- More complex testing requirements

## Implementation Notes
- The outbox relay becomes a critical component requiring robust monitoring (outbox_table_depth metric)
- Integration tests must validate the complete flow: API → DB write → outbox → relay → Kafka → consumer
- Production deployments need careful attention to transactional.id management during rolling updates

## References
- Martin Kleppmann: "Designing Data-Intensive Applications"
- Confluent blog: "Exactly-Once Semantics are Possible"
- Debezium documentation for outbox pattern implementation