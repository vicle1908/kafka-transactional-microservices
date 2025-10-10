# Debezium Connector Runbook

## Purpose

Provide step-by-step guidance for deploying and operating Debezium outbox connectors that publish Avro payloads to Kafka. Also document the polling relay mechanism that serves as a fallback when Debezium is not available.

## Setup Checklist

1. Ensure Schema Registry is reachable; capture credentials in Vault.
2. Export shared Avro schemas: `./gradlew exportAvroSchemas`.
3. Publish schemas using `REGISTRY_URL=<url> ./scripts/schema-publish.sh` before enabling connectors.
4. Apply connector manifests under `infra/debezium/connectors/` (Helm/Terraform) for service-specific connectors or `infra/debezium/outbox-connector.json` for generic configuration.
5. Verify Debezium tasks in Kafka Connect REST API return `RUNNING`.
6. For services using the polling relay fallback, ensure the `outbox.relay.enabled=true` property is set.
7. Refer to the Connector Configuration Guide (`docs/runbooks/connector-configuration-guide.md`) for detailed information about the different connector configurations.

## Health Checks

- Monitor connector status (`/connectors/<name>/status`).
- Track lag metrics via Prometheus (task state, queue size, snapshot delay).
- Use Grafana dashboard panels (Phase 3 deliverable) for lag and error rates.
- For services using the polling relay, monitor the health endpoint which reports status based on pending/failed message counts.

## Replay Procedure

1. Identify events needing replay (event ID or timeframe).
2. Run `scripts/outbox-replay.sh --message-id <uuid>` to replay a specific message.
3. Run `scripts/outbox-replay.sh --time-range <start> <end>` to replay messages within a time range.
4. Run `scripts/outbox-replay.sh --service <service-name>` to replay messages for a specific service.
5. Alternatively, use the REST API endpoints in the outbox relay service:
   - `POST /api/outbox/process` - Process all pending messages
   - `POST /api/outbox/replay/{messageId}` - Replay a specific message
   - `POST /api/outbox/replay-range?startTime=<start>&endTime=<end>` - Replay messages in a time range
6. Confirm events are reprocessed (monitor topic offsets and connector logs).
7. Ensure connectors use appropriate converters; verify the registry has the expected schema version before replaying.

## Bootstrap Summary

1. `./gradlew exportAvroSchemas`
2. `REGISTRY_URL=<url> ./scripts/schema-publish.sh`
3. Deploy connectors from `infra/debezium/connectors/` for service-specific deployment or `infra/debezium/outbox-connector.json` for generic configuration
4. Monitor with Grafana dashboard (`infra/grafana/debezium-lag-dashboard.json`)

## Alert Response

- **Connector DOWN**: Restart task, confirm Kafka/DB connectivity, review logs.
- **Schema compatibility failure**: Roll back schema change, run `exportAvroSchemas`, revalidate compatibility in CI.
- **Lag spike**: Inspect DB load, connector logs, Kafka throughput; consider scaling Connect workers.
- **High pending messages in polling relay**: Check Kafka connectivity, review relay service logs, consider manually triggering processing.
- **Failed messages in polling relay**: Review error logs, fix underlying issues, replay failed messages.

## Troubleshooting

- When configuring Kafka consumers in services, ensure you're using `containerProperties.kafkaAwareTransactionManager` instead of the deprecated `transactionManager` property to avoid runtime errors.
- For services using the polling relay, check that the `outbox.relay.enabled` property is set to `true`.
- If messages are not being processed by the polling relay, verify that the scheduled processor is running and check the service logs for errors.

## Polling Relay Mechanism

As a fallback to Debezium, services can use the polling relay mechanism implemented in the `common-outbox-relay` module. This mechanism provides scheduled and on-demand processing of outbox messages.

### Features

1. **Scheduled Processing**: Automatically processes pending messages every 5 seconds
2. **REST API Endpoints**: Manual processing and replay capabilities
3. **Metrics Collection**: Built-in monitoring and alerting
4. **Health Indicators**: System status visibility

### REST API Endpoints

- `POST /api/outbox/process` - Process all pending messages
- `POST /api/outbox/replay/{messageId}` - Replay a specific message
- `POST /api/outbox/replay-range?startTime=<start>&endTime=<end>` - Replay messages in a time range
- `GET /api/outbox/pending-count` - Get the current count of pending messages

### Configuration

Enable the polling relay by setting the following property:

```
outbox.relay.enabled=true
```

### Metrics

The polling relay collects the following metrics:

- `outbox.pending.messages` - Gauge showing the number of pending messages
- `outbox.processed.messages` - Counter for successfully processed messages
- `outbox.failed.messages` - Counter for failed messages
- `relay.kafka.commit.latency` - Timer for Kafka transaction commit latency
- `outbox.end.to.end.latency` - Timer for end-to-end processing latency

### Health Indicators

The polling relay provides health information through Spring Boot Actuator. It reports:

- UP status when pending messages are below threshold
- WARNING status when pending messages exceed 1000
- DOWN status when pending messages exceed 5000

## Monitoring and Observability

### Grafana Dashboards

The system includes two main Grafana dashboards for monitoring Debezium and outbox processing:

1. **Outbox and Transaction Monitoring** (`infra/grafana/dashboards/outbox-monitoring.json`):
   - Outbox Table Depth
   - Kafka Consumer Lag
   - Transaction Commit Rate
   - Relay Performance
   - End-to-End Latency
   - Debezium Connector Health
   - Polling Relay Metrics

2. **Outbox Relay and Debezium Monitoring** (`infra/grafana/dashboards/outbox-debezium-monitoring.json`):
   - Outbox Table Depth
   - Kafka Consumer Group Lag
   - Transaction Commit Rate
   - Relay Performance
   - End-to-End Latency
   - Debezium Connector Health
   - Debezium Offset Lag
   - Connector Error Rates

### Alerting Rules

The following alerting rules are configured:

1. **High Outbox Depth Alert**: Triggers when outbox table depth exceeds 1000 messages for 5 minutes
2. **High Debezium Lag Alert**: Triggers when Debezium offset lag exceeds 10000 records
3. **High Connector Error Rate Alert**: Triggers when connector error rate exceeds 0.05 errors per second

## References

- Debezium Outbox Event Router docs
- Schema Registry compatibility guide
- Internal ADRs (`docs/adrs/0001-transactional-outbox.md`)
- Spring for Apache Kafka documentation on transaction management
- Polling Relay Runbook (`docs/runbooks/polling-relay.md`)
- Connector Configuration Guide (`docs/runbooks/connector-configuration-guide.md`)
- Grafana Dashboards (`infra/grafana/dashboards/`)
