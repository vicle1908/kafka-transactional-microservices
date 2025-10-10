# Infrastructure and GitHub Setup Summary

## Infrastructure Overview

### Docker Compose Setup (infra/compose.yml)

The local development infrastructure includes:

**Kafka (Apache Kafka 4.1.0)**

- Node ID: 1
- KRaft mode (no ZooKeeper required)
- Listeners: PLAINTEXT (9092), CONTROLLER (9093)
- Transaction log replication factor: 1
- Auto-create topics: disabled

**Schema Registry (Confluent 7.7.0)**

- Port: 8081
- Connected to Kafka for Avro schema management
- Stores schemas for event contracts

**PostgreSQL (v18)**

- Port: 5432
- Database: orders
- User: app / Password: app
- Used for microservice data persistence

**Debezium Connect (v3.3)**

- Port: 8083
- Enables CDC (Change Data Capture) from database to Kafka
- Uses Avro converters for schema management
- Configured for transactional outbox pattern

**AKHQ (v0.24.0)**

- Port: 8080
- Kafka management and monitoring UI
- Includes schema registry integration

## GitHub Actions Workflows

### CI Workflow (.github/workflows/ci.yml)

**Triggers**: Push to main branch, Pull Requests to main
**Jobs**:

- **Build**: Validates JDK 25, runs Gradle version check, schema compatibility, and test suite
- **Docs**: Runs markdown linting on all documentation files
- **Caching**: Uses Gradle cache with configuration cache enabled
- **Security**: Includes dependency review action

### Schema Compatibility Workflow (.github/workflows/schema-compatibility.yml)

**Triggers**: Changes to common-events-avro, gradle/libs.versions.toml, or schema workflow
**Purpose**: Validates Avro schema compatibility and compilation
**Steps**: Exports schemas, runs compatibility checks

## Project Architecture

### Multi-Module Gradle Structure

- **common-events**: Shared event definitions
- **common-kafka**: Kafka configuration and transaction management
- **common-persistence**: JPA entities, repositories, transactional outbox
- **common-sagas**: Saga state management
- **common-events-avro**: Avro event contracts
- **services/**: Individual microservices
  - orders-service
  - payments-service
  - inventory-service
  - notification-service

### Key Technical Decisions

- **Language**: Kotlin 2.2.20 on Java 25
- **Framework**: Spring Boot 3.5.6
- **Messaging**: Apache Kafka 4.1.0 with Spring for Apache Kafka 3.3.10
- **Persistence**: Spring Data JPA with PostgreSQL
- **Event Serialization**: Apache Avro with Schema Registry
- **Transaction Management**: KafkaTransactionManager for exactly-once semantics
- **Outbox Pattern**: Transactional outbox with Debezium for event publishing

## Development Standards

- **Code Quality**: ktlint, Detekt, Jacoco coverage
- **Build**: Gradle 9.1.0 with configuration cache enabled
- **Testing**: Unit, integration, and contract tests with Testcontainers
- **Configuration**: Version catalog in gradle/libs.versions.toml

## Security & Operations

- **Exactly-Once Semantics**: Idempotent producers and read_committed consumers
- **Processed Events Ledger**: Per-service table to prevent duplicate processing
- **Gradle Security**: Configuration cache with integrity checks
- **Infrastructure**: Containerized with Docker Compose for consistency

## Implementation Phases

Following the phased roadmap from IMPLEMENTATION_PLAN.md:

- **Phase 0**: Discovery & Architecture complete
- **Phase 1**: Platform Foundation (Kafka, PostgreSQL, Debezium setup) complete
- **Phase 2**: Service Template & Shared Components (ongoing)
- **Phase 3**: Outbox Relay & Tooling (upcoming)
- **Phase 4**: Service Implementations (planned)
- **Phase 5**: Observability & Resilience (planned)
- **Phase 6**: Hardening & Launch (planned)

This infrastructure provides a solid foundation for transactional microservices with Kafka-based event streaming, supporting the exactly-once semantics required for business-critical state changes.
