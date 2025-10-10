# Outbox Relay and Replay Procedures

This document describes the outbox relay mechanism and procedures for replaying outbox messages.

## Outbox Relay Mechanism

The system implements two approaches for the outbox pattern:

1. **Debezium CDC with Outbox Event Router SMT** (default for high-volume services)
2. **Custom Polling Relay** (fallback for low-volume services)

### Debezium CDC Approach

The Debezium approach uses the Outbox Event Router Single Message Transform to automatically capture changes to the outbox table and publish them to Kafka.

#### Configuration

The Debezium connector monitors the `outbox` table and uses the Outbox Event Router SMT to transform database change events into business events.

Key configuration elements:

- `transforms.outbox.type`: `io.debezium.transforms.outbox.EventRouter`
- `transforms.outbox.table.fields.additional.placement`: Maps outbox table columns to event fields
- `value.converter`: `io.confluent.connect.avro.AvroConverter` for Avro schema serialization

#### Topics Routing

Events are routed to topics based on the `aggregate_type` field:

- Orders: `orders` topic
- Payments: `payments` topic
- Inventory: `inventory` topic
- Notifications: `notifications` topic

### Custom Polling Relay

The custom polling relay is implemented in the `common-outbox-relay` module and provides a simpler alternative for services that don't require the full Debezium infrastructure.

#### Architecture

The polling relay consists of:

1. `OutboxMessage` entity for representing outbox records
2. `OutboxRepository` for database access
3. `OutboxRelayService` for processing outbox messages
4. `ScheduledOutboxProcessor` for periodic processing
5. `OutboxController` for manual operations

#### Processing Flow

1. The scheduled processor runs every 5 seconds (configurable)
2. It queries for pending outbox messages ordered by creation time
3. For each message, it:
   - Determines the appropriate Kafka topic
   - Extracts the message key and payload
   - Sends the message within a Kafka transaction
   - Marks the message as sent in the database

## Replay Procedures

The system provides multiple ways to replay outbox messages for recovery, testing, or debugging purposes.

### Manual Replay via API

Services expose REST endpoints for manual replay operations:

```
POST /api/outbox/process
Replay all pending messages

POST /api/outbox/replay/{messageId}
Replay a specific message by ID

POST /api/outbox/replay-range?startTime={start}&endTime={end}
Replay messages within a time range

GET /api/outbox/pending-count
Get the current count of pending messages
```

### Automated Replay via Script

The `scripts/outbox-replay.sh` script provides a command-line interface for replay operations:

```bash
# Replay a specific message
./scripts/outbox-replay.sh --message-id 123e4567-e89b-12d3-a456-426614174000

# Replay messages in a time range
./scripts/outbox-replay.sh --time-range 2025-10-01T00:00:00 2025-10-01T01:00:00

# Replay messages for a specific service
./scripts/outbox-replay.sh --service orders
```

### Bulk Replay Operations

For large-scale replay operations, use the time-range approach to process messages in batches:

1. Identify the time range of messages to replay
2. Use the script or API to initiate the replay
3. Monitor the process using the monitoring dashboards
4. Verify successful replay through service logs and Kafka topic inspection

## Monitoring and Alerting

### Key Metrics

1. **Outbox Table Depth**: Number of pending messages awaiting processing
2. **Relay Kafka Commit Latency**: Time taken to commit Kafka transactions
3. **End-to-End Latency**: Time from message creation to successful publication
4. **Debezium Lag**: Offset lag for Debezium connectors
5. **Connector Health**: Status and uptime of Debezium connectors

### Alerting Thresholds

- **High Outbox Depth**: Alert when pending messages exceed 1000 for more than 5 minutes
- **Connector Down**: Alert immediately when any Debezium connector goes down
- **High Latency**: Alert when average commit latency exceeds 100ms for more than 15 minutes
- **Processing Failures**: Alert on consecutive processing failures

## Troubleshooting

### Common Issues

1. **Messages Stuck in Pending State**
   - Check Kafka connectivity and authentication
   - Verify message format and serialization
   - Review service logs for processing errors

2. **Debezium Connector Lag**
   - Check database performance and resource usage
   - Review connector configuration and tuning parameters
   - Monitor network connectivity between Debezium and Kafka

3. **Duplicate Messages**
   - Verify Kafka producer idempotence settings
   - Check consumer isolation levels
   - Review transaction boundaries in service code

### Diagnostic Procedures

1. **Check Outbox Table Contents**

   ```sql
   SELECT * FROM outbox WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 10;
   ```

2. **Verify Kafka Topic Contents**

   ```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning
   ```

3. **Check Debezium Connector Status**

   ```bash
   curl http://localhost:8083/connectors/orders-outbox-connector/status
   ```

## Best Practices

### Message Design

- Keep outbox messages small and focused
- Use appropriate aggregate types for topic routing
- Include sufficient context for downstream consumers
- Validate message content before writing to outbox

### Error Handling

- Implement comprehensive error handling in relay services
- Use circuit breakers for external dependencies
- Log detailed error information for troubleshooting
- Implement dead letter queues for persistent failures

### Performance Optimization

- Tune polling intervals based on message volume
- Optimize database indexes for outbox queries
- Monitor and adjust Kafka producer/consumer configurations
- Use connection pooling for database access

### Security Considerations

- Protect outbox table access with appropriate permissions
- Encrypt sensitive data in outbox messages
- Use secure communication channels for Kafka connections
- Implement proper authentication for replay APIs
