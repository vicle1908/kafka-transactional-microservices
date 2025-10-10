# Infrastructure Setup Guide

This document describes the infrastructure setup for the Kafka Transactional Microservices project, including local development, testing, and production-like environments.

## Local Development Environment

### Docker Compose (infra/compose.yml)
The default development environment includes:
- **Kafka**: Single-node cluster with KRaft mode for transactional support
- **Schema Registry**: Confluent Schema Registry for Avro schema management
- **PostgreSQL**: Database for microservices data and outbox tables
- **Debezium Connect**: For outbox event publishing with Outbox Event Router SMT
- **AKHQ**: Kafka web UI for monitoring and management
- **Redis**: Caching layer for high-read workloads

### Key Configuration for Exactly-Once Semantics (EOS)
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR`: 1 (for development)
- `KAFKA_TRANSACTION_STATE_LOG_MIN_ISR`: 1 (for development)
- `KAFKA_MIN_INSYNC_REPLICAS`: 1 (for development)
- `KAFKA_DEFAULT_REPLICATION_FACTOR`: 1 (for development)

## Integration Testing Environment

### Docker Compose (infra/compose.test.yml)
The test environment mirrors the development setup but optimized for CI/CD:
- **PostgreSQL Test DB**: Separate database instance for tests
- **Kafka Test Cluster**: Single-node KRaft cluster with EOS configuration
- **Schema Registry Test**: Separate instance for test schemas
- **Debezium Connect Test**: Isolated connector for test outbox processing
- **Redis Test**: Isolated cache for test environments

### GitHub Actions Integration
The integration test workflow includes:
- Complete Kafka/DB environment setup
- EOS validation tests
- End-to-end transaction flow verification
- Outbox depth monitoring during tests

## Production-Like Environment

### Docker Compose (infra/compose.prod.yml)
The production-like setup includes:
- **Kafka Cluster**: 3-node cluster with proper replication for EOS
- **Schema Registry**: Production-grade configuration
- **PostgreSQL**: Production database with proper initialization
- **Debezium Connect**: Multi-node setup for reliability
- **Redis**: Production caching configuration
- **Monitoring Stack**: Prometheus + Grafana for observability

### Production Configuration for EOS
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR`: 3
- `KAFKA_TRANSACTION_STATE_LOG_MIN_ISR`: 2
- `KAFKA_MIN_INSYNC_REPLICAS`: 2
- `KAFKA_DEFAULT_REPLICATION_FACTOR`: 3

## Monitoring and Observability

### Key Metrics
- `outbox_table_depth`: Number of pending outbox records
- `kafka_consumer_lag`: Consumer lag across all topics
- `kafka_transaction_commit_total`: Transaction commit rate
- `relay_kafka_commit_latency`: Outbox relay performance
- `end_to_end_latency`: Complete message processing time

### Dashboards
- Outbox monitoring dashboard with alerting for high depth
- Kafka performance and transaction metrics
- Debezium connector health monitoring
- Service-specific metrics for each microservice

## Running the Environments

### Local Development
```bash
# Start local development environment
docker-compose -f infra/compose.yml up -d

# Stop local development environment
docker-compose -f infra/compose.yml down
```

### Integration Testing
```bash
# Start test environment
docker-compose -f infra/compose.test.yml up -d

# Run integration tests with full environment
./gradlew integrationTest
```

### Production-Like
```bash
# Start production-like environment
docker-compose -f infra/compose.prod.yml up -d

# Monitor with Grafana at http://localhost:3000
# Monitor with Prometheus at http://localhost:9090
```

## Security Considerations
- Schema Registry authentication (to be configured for production)
- Kafka security (SASL/SSL) for production
- Network isolation using Docker networks
- Proper resource limits and health checks

## Operational Considerations
- Regular monitoring of outbox table depth
- Alerting for high consumer lag
- Backup strategies for PostgreSQL
- Kafka cluster health monitoring
- Schema evolution and compatibility checks