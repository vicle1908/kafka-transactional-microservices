# Kafka Transactional Microservices

This project provides a reference implementation of Kafka-backed microservices that guarantee atomic business state updates and message publication. It utilizes the transactional outbox pattern with Debezium-based relays for cross-service messaging with exactly-once semantics.

## Objectives

- Deliver Kafka-backed microservices that guarantee atomic business state updates and message publication.
- Standardize on the transactional outbox pattern with Debezium-based relays for cross-service messaging with exactly-once semantics.
- Provide observability, resiliency, and operational runbooks to support production deployment.
- Implement comprehensive integration testing to validate end-to-end transactional guarantees.

## Tech Stack

- Spring Boot 3.5.6
- Kotlin 2.2.20 on Java 25 (fallback to Java 23/21 where required)
- Kafka 4.1.0
- PostgreSQL 18
- Debezium 3.3.0.Final

## Getting Started

### Prerequisites

- Java 25
- Docker

### Building the project

```bash
./gradlew build
```

### Running the services

To start the infrastructure (Kafka, PostgreSQL, Debezium, etc.), run:

```bash
docker-compose -f infra/compose.yml up -d
```

To run a specific service, for example `orders-service`:

```bash
./gradlew :services:orders-service:bootRun
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
