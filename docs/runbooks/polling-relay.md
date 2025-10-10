# Polling Relay Runbook

## Purpose

This document provides guidance for operating the polling relay mechanism that serves as a fallback to Debezium for outbox event publishing.

## Overview

The polling relay mechanism is implemented in the `common-outbox-relay` module and provides:

1. Scheduled processing of pending outbox messages
2. REST API endpoints for manual operations
3. Metrics collection for monitoring
4. Health indicators for system status

## Setup Checklist

1. Ensure the `common-outbox-relay` dependency is included in the service
2. Set the property `outbox.relay.enabled=true` to enable the relay mechanism
3. Configure Kafka connection properties for the service
4. Verify database connectivity for accessing the outbox table

## Scheduled Processing

The polling relay automatically processes pending messages every 5 seconds. This is implemented in the `ScheduledOutboxProcessor` component.

### Configuration

The scheduled processing can be configured with the following properties:

```
# Enable/disable the polling relay
outbox.relay.enabled=true

# Scheduled processing interval (default: 5000ms)
outbox.relay.scheduled.interval=5000
```

## REST API Endpoints

The polling relay provides REST API endpoints for manual operations:

### Process PendingMessages

```
POST /api/outbox/process
```

Triggers processing of all pending messages in the outbox table.

### Replay Specific Message

```
POST /api/outbox/replay/{messageId}
```

Replays a specific message by its ID.

### Replay Messages in Time Range

```
POST/api/outbox/replay-range?startTime=<start>&endTime=<end>
```

Replays all messages within a specified time range.

### Get Pending Message Count

```
GET /api/outbox/pending-count
```

Returns the current count of pending messages in the outbox table.

## Metrics

The pollingrelay collects the following metrics:

| Metric Name | Type | Description |
|-------------|------|-------------|
| `outbox.pending.messages` | Gauge | Number of pending messages awaiting processing |
| `outbox.processed.messages` | Counter | Number of successfully processed messages |
| `outbox.failed.messages` | Counter |Number of failed messages |
| `relay.kafka.commit.latency` | Timer | Time taken to commit Kafka transactions |
| `outbox.end.to.end.latency` | Timer | End-to-end processing time from message creation to publication |

## Health Indicators

The polling relay provides health information through Spring Boot Actuator:

-**UP**: When pending messages are below threshold (1000)

- **WARNING**: When pending messages exceed 1000
- **DOWN**: When pending messages exceed 5000

Health details include:

- `pendingMessages`: Current count of pending messages
- `failedMessages`: Current countof failed messages
- `status`: Textual status (OK, WARNING, CRITICAL)

## Alert Response

- **High pending messages**: Check Kafka connectivity, review relay service logs, consider manually triggering processing
- **Failed messages**: Review error logs, fix underlying issues, replay failed messages
- **Service DOWN**:Check service availability, restart if necessary, verify database and Kafka connectivity

## Troubleshooting

### Messages Not Processing

1. Verify that `outbox.relay.enabled=true` is set
2. Check service logs for errors in the scheduled processor
3. Verify Kafka connectivity
4. Check database connectivity and outboxtable access

### High Latency

1. Check Kafka cluster performance
2. Review database performance for outbox table queries
3. Monitor system resources (CPU, memory, disk I/O)

### Failed Messages

1. Review service logs for specific error messages
2. Check Kafka topic permissions
3. Verify messagepayload format
4. Check database constraints that might prevent status updates

## Maintenance Procedures

### Manual Processing

To manually trigger processing of pending messages:

```bash
curl -X POST http://<service-host>:<port>/api/outbox/process
```

### Message Replay

To replay a specific message:

```bashcurl -X POST http://<service-host>:<port>/api/outbox/replay/<message-id>
```

### Time Range Replay

To replay messages in a time range:

```bash
curl -X POST "http://<service-host>:<port>/api/outbox/replay-range?startTime=2025-10-01T00:00:00Z&endTime=2025-10-01T01:00:00Z"
```

## Configuration Reference

### Application Properties

```properties
# Enable polling relay
outbox.relay.enabled=true

# Scheduled processing interval in milliseconds
outbox.relay.scheduled.interval=5000

# Kafka configuration
spring.kafka.bootstrap-servers=<kafka-bootstrap-servers>
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# Database configuration
spring.datasource.url=<database-url>
spring.datasource.username=<username>
spring.datasource.password=<password>
```

### Avro Schema Handling

The polling relay currently sends message payloads as strings. To enable Avro serialization:

1. Add the Avro dependency to your service:

```kotlin
implementation("org.apache.avro:avro:1.11.3")
```

2. Configure the Kafka producer to use Avroserialization:

```properties
spring.kafka.producer.key-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.properties.schema.registry.url=http://schema-registry:8081
```

3. Update the payload conversion logic in `OutboxRelayService.convertPayload()` method to serialize your objects to Avro.

## Customization

### Topic Routing

To customize how messages are routed to topics, override the `determineTopic()` method in `OutboxRelayService`.

### Message Keys

Tocustomize message keys, modify the `extractKey()` method in `OutboxRelayService`.

### Payload Processing

To customize how payloads are processed, modify the `convertPayload()` method in `OutboxRelayService`.

## References

- ADR 0005: Polling Relay Mechanism as Debezium Fallback
- Debezium runbook (`docs/runbooks/debezium.md`)
- Spring Boot documentation on scheduling
- Spring Boot Actuator documentation
-Connector Configuration Guide (`docs/runbooks/connector-configuration-guide.md`)
