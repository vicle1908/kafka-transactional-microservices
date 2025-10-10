# ADR 0002: Debezium for Outbox Event Publishing

## Status

Accepted

## Context

The transactional outbox pattern requires a reliable mechanism to read from the `outbox` table and publish messages to Kafka. Two primary approaches exist:

1. **Custom Polling Service**: A service that periodically queries the outbox table for PENDING records and publishes them to Kafka
2. **Change Data Capture (CDC)**: A service that captures changes to the outbox table in real-time and publishes corresponding messages

We need to select the approach that provides the best balance of performance, reliability, and operational simplicity.

## Decision

We will use Debezium with the Outbox Event Router SMT (Single Message Transform) to implement the outbox message relay for production services, while retaining a lightweight polling mechanism for low-volume services.

## Comparison

### Custom Polling Service

**Pros:**

- Simple to implement and understand
- Direct control over polling frequency and batch sizes
- Easy to add custom logic and error handling
- Lower operational overhead (no additional CDC infrastructure)

**Cons:**

- Potential latency between DB commit and Kafka publication based on polling interval
- Possible resource waste with frequent polling during low activity
- More complex error handling for partial failures
- Less efficient for high-throughput scenarios

### Debezium CDC

**Pros:**

- Near real-time event publication (milliseconds vs seconds)
- More efficient for high-throughput scenarios
- Built-in error handling, retries, and dead letter queue support
- Proven scalability and reliability in production systems
- Automatic handling of offsets and failure recovery

**Cons:**

- Additional operational complexity (Kafka Connect, connectors to manage)
- Requires more infrastructure components (Connect cluster, etc.)
- More complex setup and configuration
- Learning curve for operations team

## Debezium Configuration for Outbox Pattern

### Connector Configuration

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgreSQLConnector",
    "tasks.max": "1",
    "database.hostname": "${database.hostname}",
    "database.port": "5432",
    "database.user": "debezium_user",
    "database.password": "${database.password}",
    "database.dbname": "orders",
    "database.server.name": "outbox-server",
    "table.include.list": "public.outbox",
    "plugin.name": "pgoutput",
    "publication.name": "outbox_publication",
    "snapshot.mode": "never",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.headers": "headers",
    "transforms.outbox.route.topic.replacement": "${routetopic.replacement}",
    "transforms.outbox.route.by.field": "aggregate_type",
    "transforms.outbox.operation.routing.enabled": "false"
  }
}
```

### Event Router SMT Features

- Maps outbox table records to Kafka events automatically
- Extracts event key, payload, and type from table columns
- Routes to topics based on aggregate type
- Handles headers and metadata propagation
- Provides built-in deduplication support

## Implementation Strategy

### Phase 1: Core Services

- Use Debezium for Order, Payment, and Inventory services (high volume)
- Implement proper monitoring for connector lag and health

### Phase 2: Low-Volume Services

- Use lightweight polling service for Notification service (low volume)
- Feature flag to enable/disable poller vs Debezium

## Operational Considerations

### Monitoring Requirements

- Debezium connector lag (time between DB commit and Kafka publication)
- Connector health and status
- Outbox table depth monitoring
- Error rates and dead letter queues

### Security

- Secure Kafka Connect cluster
- Database user with minimal required permissions
- Network isolation for Connect cluster

## Alternatives Considered

### Custom Polling Service

- Discarded for core services due to latency and throughput limitations
- Retained for low-volume services where simplicity is preferred

### Other CDC Solutions

- StreamThoughts Kafka Connect JDBC Source Connector
- Custom application-level CDC using PostgreSQL logical replication
- Chose Debezium due to its maturity, community support, and Outbox Event Router SMT

## Consequences

### Positive

- Near real-time event publication for core services
- Proven scalability and reliability
- Built-in error handling and recovery mechanisms
- Standardized approach across services

### Negative

- Additional operational complexity
- More infrastructure components to maintain
- Learning curve for operations
- Potential vendor lock-in to Debezium ecosystem

## References

- Debezium Outbox Event Router documentation
- "Pattern: Outbox pattern for microservices" - microservices.io
- Debezium connector for PostgreSQL documentation
