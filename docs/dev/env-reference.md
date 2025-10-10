# Environment Variable Reference

This repository uses 12‑Factor, environment‑driven configuration for local/dev. Real secrets must NOT be committed to the repo; staging/production secrets come from Vault (see ADR‑0004 and docs/runbooks/vault.md). The .env files are for local development only.

Loading order and usage

- Root application env (local/dev):
  - Copy once: `cp .env.example .env`
  - Auto‑load with direnv: `direnv allow` (recommended)
  - Or shell fallback: `source scripts/export-env.sh` (per shell)
- Docker Compose env (infra):
  - Copy once: `cp infra/.env.example infra/.env`
  - Compose auto‑loads infra/.env when run from infra/, or pass `--env-file infra/.env` from repo root

Global app variables (.env, root)

- SPRING_PROFILES_ACTIVE: active Spring profile; default local
- DB_HOST, DB_PORT, DB_USER, DB_PASSWORD: shared DB connection defaults (dev‑safe)
- ORDERS_DB_NAME, ORDERS_DB_URL: orders DB name and JDBC URL; services resolve `ORDERS_DB_URL` with fallback `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${ORDERS_DB_NAME}`
- PAYMENTS_DB_NAME, PAYMENTS_DB_URL: payments DB variables
- INVENTORY_DB_NAME, INVENTORY_DB_URL: inventory DB variables
- NOTIFICATIONS_DB_NAME, NOTIFICATIONS_DB_URL: notifications DB variables
- KAFKA_BOOTSTRAP_SERVERS: Kafka brokers for services (e.g., localhost:9092)
- SCHEMA_REGISTRY_URL: Confluent Schema Registry endpoint (e.g., <http://localhost:8081>)
- DEBEZIUM_CONNECT_URL: Debezium Connect REST endpoint (e.g., <http://localhost:8083>)
- REDIS_HOST, REDIS_PORT: Redis connection for services (if used)
- REDIS_USERNAME, REDIS_PASSWORD: Optional credentials for non-local environments
- REDIS_SSL: Set to true to enable TLS (common-cache will configure Lettuce with SSL)
- OTEL_ENABLED: enable/disable tracing export (default false)
- OTEL_SERVICE_NAME: logical service name for telemetry (default my-kafka-microservice)
- OTEL_ENDPOINT: OTLP exporter endpoint (host:port, gRPC 4317 / HTTP 4318); services prepend http:// when needed
- ORDERS_KAFKA_TX_PREFIX, PAYMENTS_KAFKA_TX_PREFIX, INVENTORY_KAFKA_TX_PREFIX, NOTIFICATION_KAFKA_TX_PREFIX: per‑service transactional producer prefixes
- TEMPORAL_TARGET: Temporal server address for workflow engines (default 127.0.0.1:7233)

Per‑service configuration sources (application.yml)

- Orders service reads:
  - Datasource: `${ORDERS_DB_URL}` with fallback via `${DB_HOST}/${DB_PORT}/${ORDERS_DB_NAME}`
  - Kafka: `${KAFKA_BOOTSTRAP_SERVERS}`
  - Schema Registry: `${SCHEMA_REGISTRY_URL}`
  - Tx prefix: `${ORDERS_KAFKA_TX_PREFIX}`
- Payments service reads:
  - Datasource: `${PAYMENTS_DB_URL}` (fallback to DB_* + PAYMENTS_DB_NAME)
  - Kafka/Schema Registry: `${KAFKA_BOOTSTRAP_SERVERS}`, `${SCHEMA_REGISTRY_URL}`
  - Tx prefix: `${PAYMENTS_KAFKA_TX_PREFIX}`
- Inventory service reads:
  - Datasource: `${INVENTORY_DB_URL}` (fallback to DB_* + INVENTORY_DB_NAME)
  - Kafka/Schema Registry: `${KAFKA_BOOTSTRAP_SERVERS}`, `${SCHEMA_REGISTRY_URL}`
  - Tx prefix: `${INVENTORY_KAFKA_TX_PREFIX}`
  - Temporal: `${TEMPORAL_TARGET}`
- Notification service reads:
  - Datasource: `${NOTIFICATIONS_DB_URL}` (fallback to DB_* + NOTIFICATIONS_DB_NAME)
  - Kafka/Schema Registry: `${KAFKA_BOOTSTRAP_SERVERS}`, `${SCHEMA_REGISTRY_URL}`
  - Tx prefix: `${NOTIFICATION_KAFKA_TX_PREFIX}`
- Temporal pilot reads:
  - Management tracing: `${OTEL_ENABLED}`, `${OTEL_ENDPOINT}`
  - Temporal server target: `${TEMPORAL_TARGET}`

Compose variables (infra/.env)

- Kafka: `KAFKA_IMAGE`, `KAFKA_PLAINTEXT_PORT`, `KAFKA_CONTROLLER_PORT`
- Schema Registry: `SCHEMA_REGISTRY_IMAGE`, `SCHEMA_REGISTRY_PORT`
- Postgres: `POSTGRES_IMAGE`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB_ORDERS`
- Redis: `REDIS_IMAGE`, `REDIS_PORT`
- Debezium Connect: `DEBEZIUM_IMAGE`, `DEBEZIUM_PORT`
- AKHQ: `AKHQ_IMAGE`, `AKHQ_PORT`

Defaults & fallbacks

- Services include safe fallbacks (e.g., localhost:9092) to run out‑of‑the‑box after copying .env.example.
- You can override any variable in your local .env without touching YAML files.

Examples

- Change DB port for local Postgres:

  ```env
  DB_PORT=5433
  ORDERS_DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${ORDERS_DB_NAME}
  ```

- Use a remote Kafka broker:

  ```env
  KAFKA_BOOTSTRAP_SERVERS=broker1:9092,broker2:9092
  ```

- Enable tracing to local collector:

  ```env
  OTEL_ENABLED=true
  OTEL_ENDPOINT=localhost:4317
  ```

Security & secrets policy

- Never commit .env; it is already git‑ignored.
- Use .env only for non‑sensitive dev defaults.
- Staging/Production credentials and sensitive values are provided by Vault. See ADR‑0004 and docs/runbooks/vault.md.
