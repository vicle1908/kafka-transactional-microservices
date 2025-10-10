# Shared Components Architecture

This document describes the shared components architecture for the Kafka Transactional Microservices project.

## Common Modules Overview

### 1. `common-events`
Base module for shared event definitions and utilities.

### 2. `common-events-avro`
Contains Avro schema definitions for event contracts used across services. These schemas ensure type-safe, backward-compatible event serialization.

#### Key Avro Schemas:
- `OrderEvent`: Represents order-related actions in the system
- `PaymentEvent`: Represents payment-related actions in the system
- `InventoryEvent`: Represents inventory-related actions in the system

### 3. `common-kafka`
Provides shared Kafka configuration including:
- Transactional producer configuration for exactly-once semantics
- Consumer configuration with `read_committed` isolation level
- Kafka transaction manager integration
- Error handling and retry mechanisms

### 4. `common-persistence`
Shared persistence components including:
- Database connection configuration
- Flyway migration management
- JPA entity base classes
- Transaction management utilities

### 5. `common-sagas`
Saga pattern implementation for managing long-running business processes:
- `SagaEntity`: JPA entity for storing saga state
- `SagaRepository`: Repository for saga persistence
- `SagaService`: Service for saga lifecycle management

### 6. `common-proto`
Protocol Buffer definitions for gRPC services:
- Order service contracts
- Payment service contracts
- Inventory service contracts
- Notification service contracts

### 7. `common-temporal`
Temporal workflow platform integration for complex orchestrations:
- Workflow interfaces
- Activity definitions
- Shared DTOs for workflow communication

## Transactional Outbox Pattern Implementation

The shared components support the transactional outbox pattern through:

1. **Database Schema**: The `outbox` table schema is defined in `common-persistence` with proper indexing for efficient polling.

2. **Kafka Integration**: Transactional Kafka producers and consumers are configured in `common-kafka` with exactly-once semantics.

3. **Saga Management**: The `common-sagas` module provides persistence and lifecycle management for distributed transactions.

## Exactly-Once Semantics Configuration

### Kafka Producer Configuration
- `enable.idempotence = true`
- `acks = all`
- `retries = Integer.MAX_VALUE`
- `max.in.flight.requests.per.connection = 5`
- `transactional.id` for transactional producers

### Kafka Consumer Configuration
- `isolation.level = read_committed`
- `enable.auto.commit = false`
- Manual offset management for transactional consumption

### Database Configuration
- ACID transactions for business data and outbox writes
- Proper indexing for efficient outbox polling
- Status tracking (`PENDING`, `SENT`, `FAILED`) for message relay coordination

## Service Communication Patterns

### Event-Driven Communication
Services communicate asynchronously through Kafka topics with Avro-serialized events.

### Synchronous Communication
For request-response patterns, services use gRPC with Protocol Buffers for type-safe communication.

### Workflow Orchestration
Complex business processes use Temporal for orchestrated workflows with built-in retries and compensation.

## Version Management

All shared components use semantic versioning and are managed through the Gradle version catalog in `gradle/libs.versions.toml`.