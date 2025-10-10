# Service Catalog & Data Store Inventory

| Service | Primary Data Store | Access Pattern | Consistency Requirement | Notes |
|---------|--------------------|----------------|-------------------------|-------|
| Order Service | PostgreSQL (orders, order_items) | Writes on order creation/update; reads for status | Exactly-once for order creation events; at-least-once for queries | Emits OrderCreated, OrderCancelled events via outbox. |
| Payment Service | PostgreSQL (payments) + External PSP API | Writes payment intents, reads payment status | Exactly-once for payment capture; compensations required on failure | Consumes OrderCreated; emits PaymentCompleted/Failed. |
| Inventory Service | PostgreSQL (inventory, reservations) | Reads stock levels; writes reservations | Exactly-once for reservation adjustments; compensating release events | Consumes OrderCreated/PaymentCompleted; emits InventoryReserved/Released. |
| Notification Service | Redis (rate limiting) + External providers | Reads templates/config; writes notification logs | At-least-once delivery acceptable with idempotent downstream providers | Consumes domain events; uses retry + DLQ topics. |
| Reporting Service (future) | BigQuery / Data Lake | Batch reads from Kafka sink | Eventually consistent | Optional downstream consumer for analytics. |

## Data Flow Highlights
- Order Service initiates saga; Payment and Inventory react via Kafka events.
- Debezium outbox streams database changes to Kafka topics per bounded context.
- Notification Service relies on Redis for throttling and uses CDN for asset delivery.

## Integration Constraints
- Legacy ERP sync required weekly; handled via separate integration service (out of scope for MVP).
- External PSP mandates idempotency keys within 48 hours.

## Next Steps
- Validate data retention requirements with compliance team.
- Identify PII fields for masking/tokenization before publishing events.
