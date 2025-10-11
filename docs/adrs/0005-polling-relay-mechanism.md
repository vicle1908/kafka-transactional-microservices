# ADR 0005: Polling Relay Mechanism as Debezium Fallback

## Status

Accepted

## Context

While Debezium is the preferred mechanism for publishing outbox events, we need a fallback mechanism for services that either:

1. Cannot use Debezium due to infrastructure constraints
2. Have very low message volumes where the overhead of Debezium is not justified
3. Need to replay specific messages that may have been missed by Debezium

We have implemented a polling relay mechanism in the `common-outbox-relay` module that can serve as this fallback.

## Decision

We will use the polling relay mechanism as a fallback to Debezium for outbox event publishing, with the following characteristics:

1. Scheduled processing of pending messages every 5 seconds
2. REST API endpoints for on-demand processing and replay
3. Metrics collection for monitoring and alerting
4. Health indicators for system status visibility

## Implementation Details

### Core Components

1. **OutboxRelayService**: Core service that processes outbox messages
2. **ScheduledOutboxProcessor**: Scheduled component that triggers processing
3. **OutboxController**: REST API for manual operations
4. **OutboxMetricsService**: Metrics collection and reporting
5. **OutboxRelayHealthIndicator**: Health status reporting

### Features

1. **Scheduled Processing**: Automatically processes pending messages every 5 seconds
2. **Manual Processing**: REST endpoint to trigger processing on demand
3. **Message Replay**: Ability to replay specific messages by ID or time range
4. **Metrics Collection**:
   - Pending message count gauge
   - Processed message counter
   - Failed message counter
   - Kafka relay latency timer
   - End-to-end latency timer
5. **Health Indicators**: Reports system health based on pending/failed message counts

### REST API Endpoints

- `POST /api/outbox/process` - Process all pending messages
- `POST /api/outbox/replay/{messageId}` - Replay a specific message
- `POST /api/outbox/replay-range?startTime=<start>&endTime=<end>` - Replay messages in a time range
- `GET /api/outbox/pending-count` - Get the current count of pending messages

### Configuration

The polling relay can be enabled/disabled with the property:

```properties
outbox.relay.enabled=true
```

## Benefits

1. **Fallback Capability**: Provides a reliable fallback when Debezium is not available
2. **Operational Flexibility**: Allows manual intervention when needed
3. **Monitoring**: Built-in metrics and health indicators
4. **Replay Capability**: Can replay specific messages for recovery scenarios
5. **Low Overhead**: For low-volume services, scheduled processing has minimal impact

## Trade-offs

1. **Latency**: Slightly higher latency compared to Debezium's near real-time processing
2. **Resource Usage**: Scheduled processing consumes resources even during low activity periods
3. **Complexity**: Maintaining two mechanisms increases overall system complexity

## Consequences

### Positive

- Provides a robust fallback mechanism for outbox event publishing
- Enables manual intervention and recovery operations
- Offers metrics and health monitoring for the relay mechanism
- Supports both scheduled and on-demand processing

### Negative

- Increased system complexity with two publishing mechanisms
- Potential for confusion about which mechanism is active
- Additional code to maintain and test

## References

- Debezium Outbox Event Router documentation
- ADR 0002: Debezium for Outbox Event Publishing
- Spring Boot documentation on scheduling and REST controllers
