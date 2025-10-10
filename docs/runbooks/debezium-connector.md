# Debezium Connector Operations Runbook

This document provides procedures for operating and managing Debezium connectors in the Kafka Transactional Microservices platform.

## Overview

Debezium connectors capture changes from databases and stream them to Kafka topics. In this platform, we use Debezium with the Outbox Event Router Single Message Transform (SMT) to implement the transactional outbox pattern.

## Connector Management

### Listing Connectors

```bash
# List all connectors
curl -X GET http://localhost:8083/connectors

# Get connector status
curl -X GET http://localhost:8083/connectors/{connector-name}/status

# Get connector configuration
curl -X GET http://localhost:8083/connectors/{connector-name}
```

### Creating Connectors

Connectors are defined in JSON configuration files and deployed using the Kafka Connect REST API:

```bash
# Deploy a connector
curl -X POST \
  -H "Content-Type: application/json" \
  -d @infra/debezium/connectors/orders-outbox-connector.json \
  http://localhost:8083/connectors
```

### Updating Connectors

```bash
# Update connector configuration
curl -X PUT \
  -H "Content-Type: application/json" \
  -d @infra/debezium/connectors/orders-outbox-connector.json \
  http://localhost:8083/connectors/{connector-name}/config
```

### Deleting Connectors

```bash
# Delete a connector
curl -X DELETE http://localhost:8083/connectors/{connector-name}
```

## Monitoring and Metrics

### Key Metrics to Monitor

1. **Connector Health**: Status of connector tasks (RUNNING, FAILED, PAUSED)
2. **Offset Lag**: Difference between database log position and connector position
3. **Throughput**: Number of records processed per second
4. **Error Rates**: Number of failed records and retried records
5. **Memory Usage**: Heap and non-heap memory consumption

### Monitoring Commands

```bash
# Get connector tasks status
curl -X GET http://localhost:8083/connectors/{connector-name}/status | jq '.tasks[].state'

# Get connector metrics
curl -X GET http://localhost:8083/connectors/{connector-name}/metrics

# Get connector offset information
curl -X GET http://localhost:8083/connectors/{connector-name}/offsets
```

### Alerting Thresholds

- **Connector Down**: Alert immediately when any task enters FAILED state
- **High Lag**: Alert when offset lag exceeds 10,000 records for more than 5 minutes
- **Low Throughput**: Alert when processing rate drops below 10 records/minute for more than 10 minutes
- **High Error Rate**: Alert when error rate exceeds 5% for more than 5 minutes

## Troubleshooting Procedures

### Connector Won't Start

1. Check connector configuration for syntax errors
2. Verify database connectivity and credentials
3. Check Kafka connectivity and topic permissions
4. Review connector logs for specific error messages
5. Validate that required database extensions are installed (e.g., wal2json for PostgreSQL)

### High Offset Lag

1. Check database performance and resource usage
2. Review connector configuration for tuning parameters
3. Monitor network connectivity between connector and database
4. Check for long-running transactions that block log cleanup
5. Consider scaling connector tasks if appropriate

### Processing Errors

1. Review connector logs for specific error messages
2. Check if schema compatibility issues exist
3. Verify that required SMTs are properly configured
4. Check for data format issues in outbox table
5. Review dead letter queue for failed records

### Memory Issues

1. Review JVM heap settings for Kafka Connect workers
2. Check for memory leaks in custom SMTs or transformations
3. Monitor garbage collection patterns
4. Consider reducing batch sizes or polling frequencies
5. Scale horizontally by adding more Kafka Connect workers

## Recovery Procedures

### Restarting Failed Tasks

```bash
# Restart a specific task
curl -X POST http://localhost:8083/connectors/{connector-name}/tasks/0/restart

# Restart all tasks for a connector
for task in $(curl -s http://localhost:8083/connectors/{connector-name}/tasks | jq -r '.[].id.task'); do
  curl -X POST http://localhost:8083/connectors/{connector-name}/tasks/$task/restart
done
```

### Resetting Offsets

```bash
# Pause connector before resetting offsets
curl -X PUT http://localhost:8083/connectors/{connector-name}/pause

# Reset offsets to beginning
curl -X DELETE http://localhost:8083/connectors/{connector-name}/offsets

# Resume connector
curl -X PUT http://localhost:8083/connectors/{connector-name}/resume
```

### Replaying Messages

For outbox pattern implementations, use the outbox replay procedures instead of resetting Kafka Connect offsets:

```bash
# Replay specific outbox messages using the replay script
./scripts/outbox-replay.sh --time-range 2025-10-01T00:00:00 2025-10-01T01:00:00
```

## Maintenance Procedures

### Regular Maintenance Tasks

1. **Connector Configuration Reviews**: Monthly review of connector configurations for optimization opportunities
2. **Offset Cleanup**: Periodic cleanup of old offsets to prevent storage bloat
3. **Log Rotation**: Ensure connector logs are properly rotated and archived
4. **Performance Tuning**: Quarterly performance review and tuning of connector parameters
5. **Security Audits**: Regular review of connector security configurations and access controls

### Database Maintenance Considerations

1. **Vacuum Operations**: Schedule regular vacuum operations for PostgreSQL databases
2. **Log Retention**: Ensure database transaction logs are retained long enough for connectors to process
3. **Replication Slots**: Monitor replication slot usage and prevent log buildup
4. **Index Maintenance**: Maintain proper indexes on outbox tables for efficient querying

## Scaling Considerations

### Horizontal Scaling

1. **Connector Tasks**: Increase `tasks.max` for connectors processing high volumes
2. **Kafka Connect Workers**: Add more workers to distribute connector load
3. **Database Connections**: Ensure database can handle increased connection count

### Vertical Scaling

1. **JVM Heap**: Increase heap size for Kafka Connect workers processing large batches
2. **CPU Resources**: Allocate more CPU resources for computationally intensive transformations
3. **Network Bandwidth**: Ensure sufficient network bandwidth for high-throughput scenarios

## Security Procedures

### Authentication and Authorization

1. **Connector Credentials**: Rotate database credentials regularly
2. **Kafka Authentication**: Use SASL/SSL for Kafka connections
3. **Schema Registry**: Secure Schema Registry access with authentication
4. **REST API**: Protect Kafka Connect REST API with authentication

### Data Protection

1. **Encryption**: Use TLS for all network communications
2. **Masking**: Mask sensitive data in connector configurations
3. **Auditing**: Enable auditing for connector operations
4. **Compliance**: Ensure connector operations comply with data protection regulations

## Backup and Disaster Recovery

### Configuration Backup

1. **Connector Configurations**: Regular backup of all connector configuration files
2. **Kafka Connect Settings**: Backup of Kafka Connect worker configurations
3. **Database Settings**: Backup of database configurations and security settings

### Recovery Procedures (DR)

1. **Connector Restoration**: Restore connectors from backup configurations
2. **Offset Restoration**: Restore connector offsets from backups if available
3. **Data Validation**: Validate that restored connectors process data correctly
4. **Performance Verification**: Verify that restored system meets performance requirements

## Best Practices

### Configuration Management

1. **Version Control**: Store all connector configurations in version control
2. **Environment Variables**: Use environment variables for sensitive configuration values
3. **Documentation**: Document all connector configurations and their purposes
4. **Templates**: Use configuration templates for consistent connector deployments

### Monitoring and Alerting

1. **Comprehensive Coverage**: Monitor all key connector metrics
2. **Meaningful Thresholds**: Set alert thresholds based on historical data and business requirements
3. **Automated Responses**: Implement automated responses for common issues
4. **Regular Review**: Regularly review and update monitoring configurations

### Operational Excellence

1. **Standard Procedures**: Develop and maintain standard operating procedures
2. **Training**: Ensure team members are trained on connector operations
3. **Documentation**: Keep documentation up to date with system changes
4. **Continuous Improvement**: Regularly review and improve operational processes
