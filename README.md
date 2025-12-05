# Kafka Transactional Microservices

This project provides a reference implementation of Kafka-backed microservices that guarantee atomic business state updates and message publication. It utilizes the transactional outbox pattern with Debezium-based relays for cross-service messaging with exactly-once semantics.

## Objectives

- Deliver Kafka-backed microservices that guarantee atomic business state updates and message publication.
- Standardize on the transactional outbox pattern with Debezium-based relays for cross-service messaging with exactly-once semantics.
- Provide observability, resiliency, and operational runbooks to support production deployment.
- Implement comprehensive integration testing to validate end-to-end transactional guarantees.
- **File Watcher Test**: Real-time file change monitoring verified successfully.
- **New Test Added**: Real-time indexing verification test timestamp 2025-11-20-22:02.
- **Latest Test**: File watcher and real-time indexing test timestamp 2025-11-20-23:36.
- **Final Verification**: File watcher fully operational at 2025-11-22-14:46.

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

First, create local env files (examples provided) and load them:

```bash
cp .env.example .env
cp infra/.env.example infra/.env
# Recommended
brew install direnv && direnv allow
# Or fallback per-shell
source scripts/export-env.sh
```

Start the infrastructure (Kafka, PostgreSQL, Debezium, etc.) with env:

```bash
docker compose --env-file infra/.env -f infra/compose.yml up -d
```

Run a specific service, for example `orders-service` (config resolves from .env):

```bash
./gradlew :services:orders-service:bootRun
```

For the complete list of supported variables and defaults, see `docs/dev/env-reference.md`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
