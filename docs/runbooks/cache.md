# Cache Operations Runbook

## Scope
- Operational guidance for Redis cache used by microservices (local, staging, production).
- Covers provisioning, configuration, security, monitoring, and troubleshooting.

## Local Development
- Compose service: `infra/compose.yml` defines `redis` (7-alpine) with healthcheck.
- Default connection: `REDIS_HOST=localhost`, `REDIS_PORT=6379`.
- Microservices auto-config:
  - `common-cache` module provides `RedisConnectionFactory` + `RedisCacheManager` via Spring Boot auto-configuration.
  - Reads `REDIS_HOST` and `REDIS_PORT` from environment.
  - Default TTL is 15 minutes; null values are not cached.
- Bring up Redis:
  ```bash
  docker compose --env-file .env -f infra/compose.yml --profile local up -d redis
  docker ps | grep redis
  redis-cli -h localhost -p 6379 ping
  ```

## Configuration (Prod/Staging)
- Prefer managed Redis with TLS and AUTH.
- Minimal required knobs:
  - Persistence: disabled for ephemeral caches; enable AOF if durability of cache matters (rare).
  - Maxmemory: set with eviction policy `allkeys-lru` for standard caches.
  - Network: restrict to VPC; enable ACLs with least-privilege.
- Application config variables:
  - `REDIS_HOST`, `REDIS_PORT` (required)
  - Optional future: `REDIS_USERNAME`, `REDIS_PASSWORD` (not used yet; enable in app when needed)

## Security
- Use TLS and AUTH in non-local environments.
- Store credentials in Vault/Secrets Manager; do not commit secrets.
- Restrict ACLs to commands: GET, SET, DEL, EXPIRE.
- Rotate credentials regularly; document in secret runbook.

## Observability
- Metrics to track:
  - Cache (Micrometer): hit/miss ratio, gets/puts/evictions (cache.*)
  - Redis Exporter: latency, connected clients, memory usage, key count, evictions.
- Tooling:
  - Micrometer + Prometheus (already present via common-observability)
  - Redis Exporter + Prometheus scrape; Grafana dashboard with hit/miss panels and memory usage.
- Configuration:
  - Ensure management endpoints expose metrics and prometheus; set `management.metrics.enable.cache=true` (done per service).
- Alerts:
  - Low hit ratio, high latency, high eviction rate, memory fragmentation.

## Patterns
- Primary pattern: cache-aside (lazy population) for read-mostly.
- Key naming: `<service>:<context>:<entity>:<identifier>`.
- TTL defaults to 15 minutes; set domain-specific TTLs where appropriate.
- Invalidation:
  - On writes: explicit eviction (DEL) of impacted keys via domain services.
  - On events: subscribe and evict for cross-service coherence when necessary.

## Operations
- Evict after writes (after-commit recommended):
  - Publish domain events inside transactions and handle with `@TransactionalEventListener(phase = AFTER_COMMIT)`.
  - Orders: publish `OrderChangedEvent(orderId)` to evict `orders:by-id`.
  - Inventory: publish `InventoryStockChangedEvent(sku)` to evict `inventory:stock:by-sku`.
  - Prefer event-driven eviction to ensure eviction occurs only after successful commit.
- Health check:
  ```bash
  redis-cli -h <host> -p <port> ping
  ```
- Flush (emergency only):
  ```bash
  redis-cli -h <host> -p <port> FLUSHALL
  ```
- Inspect keys:
  ```bash
  redis-cli -h <host> -p <port> --scan --pattern '<service>:*'
  ```

## Troubleshooting
- Connection refused: verify network/VPC and security groups; confirm `REDIS_HOST/PORT`.
- Timeouts: check latency and server load; tune client timeouts if required.
- Low hit ratio: validate key strategy; ensure caching annotations are applied on hot paths.
- Excess evictions: increase memory or reduce TTLs; review key cardinality.

## Change Management
- Any changes to TTLs, key strategies, or security must be reviewed and documented here.
- Keep this runbook aligned with `docs/architecture/caching.md` and `docs/dev/env-reference.md`.
