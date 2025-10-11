# Outbox Integration Testing

## Overview

This document describes how to test both the Debezium-based and polling relay mechanisms for outbox event publishing.

## Test Environments

### Local Development Environment

For local testing, use the Docker Compose setup:

```bash
docker compose -f infra/compose.yml up -d
```

This starts:

- Kafka with KRaft mode
- Schema Registry
- PostgreSQL database
- Redis cache
- Debezium Connect
- AKHQ (Kafka UI)

### Test Configuration

#### Debezium Testing

1. Deploy a Debezium connector:

```bash
curl -X POST \
  http://localhost:8083/connectors \
  -H 'Content-Type: application/json' \
  -d @infra/debezium/connectors/orders-outbox-connector.json
```

1. Verify the connector is running:

```bash
curl http://localhost:8083/connectors/orders-outbox-connector/status
```

#### Polling Relay Testing

1. Enable the polling relay in your service configuration:

```properties
outbox.relay.enabled=true
```

1. Start your service and verify the scheduled processing is working.

## Integration Test Scenarios

### Scenario 1: Basic Event Publishing with Debezium

1. Create a new order in the orders service
2. Verify that a record is inserted into the outbox table
3. Verify that Debezium captures the change and publishes to Kafka
4. Verify that the event is consumed by the payments service
5. Check metrics in Grafana dashboard

### Scenario 2: Basic Event Publishing with Polling Relay

1. Disable Debezium connector for a service
2. Enable polling relay in the service configuration
3. Create a new order in the orders service
4. Verify that a record is inserted into the outbox table
5. Verify that the polling relay processes the message and publishes to Kafka
6. Verify that the event is consumed by the payments service
7. Check metrics in Grafana dashboard

### Scenario 3: Switching from Debezium to Polling Relay

1. Start with Debezium processing events
2. Stop the Debezium connector
3. Enable polling relay in the service
4. Create new events and verify they are processed by the polling relay
5. Check for any duplicate processing

### Scenario 4: Switching from Polling Relay to Debezium

1. Start with polling relay processing events
2. Disable polling relay in the service
3. Deploy and start a Debezium connector
4. Create new events and verify they are processed by Debezium
5. Check for any duplicate processing

### Scenario 5: Replay Functionality

1. Create several events
2. Simulate a failure in the consumer
3. Use the replay functionality:
   - For Debezium: Use the `scripts/outbox-replay.sh` script
   - For Polling Relay: Use the REST API endpoints
4. Verify that events are reprocessed correctly

## Test Data Setup

### Database Schema

The outbox table has the following structure:

```sql
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    occurred_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
```

### Sample Test Data

Insert sample data directly into the outbox table:

```sql
INSERT INTO outbox (id, aggregate_id, aggregate_type, event_type, payload, occurred_at, status) 
VALUES (gen_random_uuid(), '12345', 'order', 'OrderCreated', '{"orderId":"12345","customerId":"67890","items":[{"productId":"P1","quantity":2}]}', NOW(), 'PENDING');
```

## Metrics to Monitor

### Debezium Metrics

- Connector lag
- Event processing time
- Error rates
- Throughput

### Polling Relay Metrics

- Pending message count
- Processed message count
- Failed message count
- Kafka commit latency
- End-to-end latency

## Troubleshooting

### Common Issues

#### Debezium Issues

1. **Connector not starting**:
   - Check database connectivity
   - Verify database credentials
   - Check Kafka connectivity

2. **Messages not being published**:
   - Verify outbox table structure
   - Check field mappings in connector configuration
   - Verify Schema Registry connectivity

#### Polling Relay Issues

1. **Messages not being processed**:
   - Check if the relay is enabled
   - Verify Kafka connectivity
   - Check service logs for errors

2. **High latency**:
   - Check Kafka cluster performance
   - Review database performance
   - Monitor system resources

## Automated Testing

### Unit Tests

Unit tests for the outbox components are located in each module's test directory:

- `common-outbox-relay/src/test`
- `common-persistence/src/test`

### Integration Tests

Integration tests are implemented using Testcontainers and are located in:

- `common-outbox-relay/src/integration-test`
- Service-specific integration tests in each service module

To run integration tests:

```bash
./gradlew :common-outbox-relay:integrationTest
```

## Best Practices

1. **Test Both Mechanisms**: Always test both Debezium and polling relay mechanisms to ensure they work correctly.

2. **Monitor Metrics**: Continuously monitor metrics during testing to identify performance issues.

3. **Verify Exactly-Once Semantics**: Ensure that events are processed exactly once, even when switching between mechanisms.

4. **Test Failure Scenarios**: Test scenarios where one mechanism fails and the other takes over.

5. **Document Test Results**: Keep detailed records of test results for future reference.

## References

- Debezium Runbook (`docs/runbooks/debezium.md`)
- Polling Relay Runbook (`docs/runbooks/polling-relay.md`)
- Connector Configuration Guide (`docs/runbooks/connector-configuration-guide.md`)
- Grafana Dashboard Configuration (`infra/grafana/dashboards/outbox-monitoring.json`)
