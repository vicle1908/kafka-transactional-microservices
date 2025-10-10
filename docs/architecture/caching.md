# Caching Blueprint

## Goals

- Reduce database load for high-read requests.
- Improve response latency for frequently accessed data.
- Provide predictable invalidation and consistency patterns.

## Technology

- **Redis 7.x** (managed or self-hosted) with TLS, ACLs, and persistence disabled for ephemeral caches.
- Redis Cluster for horizontal scaling; minimum three-shard deployment in production.

## Patterns

- Cache-aside (lazy population) for read-mostly data.
- Optional write-through for small, high-consistency datasets (e.g., feature flags).
- TTL defaults: 15 minutes for semi-static data, configurable per cache key.
- Key naming convention: `<service>:<bounded-context>:<entity>:<identifier>`.

## Current caches (initial)

### Eviction timing (after-commit)
- We use domain events and @TransactionalEventListener(phase = AFTER_COMMIT) to evict caches strictly after successful DB commit.
- Implemented:
  - orders-service: OrderChangedEvent → evict orders:by-id
  - inventory-service: InventoryStockChangedEvent → evict inventory:stock:by-sku

- orders-service
  - Cache name: `orders:by-id` — key: `#orderId` — source: OrdersQueryService.getOrder(orderId)
- inventory-service
  - Cache name: `inventory:stock:by-sku` — key: `#sku` — source: InventoryQueryService.getStockBySku(sku)

Eviction guidance:
- On order updates that change visible state, publish OrderChangedEvent(orderId) to trigger eviction of `orders:by-id::<orderId>` after commit.
- On inventory writes (reserve/release/reconcile), publish InventoryStockChangedEvent(sku) to trigger eviction of `inventory:stock:by-sku::<sku>` after commit.

Per-cache TTL:
- Default TTL is 15m (common-cache). If a different TTL is needed, add a RedisCacheManagerBuilderCustomizer in service to override a specific cache.
- Implemented: inventory-service sets `inventory:stock:by-sku` TTL to 3 minutes.

TTL jitter (optional):
- To avoid synchronized expiry, consider adding a small random jitter to TTL per entry.
- This requires a custom cache implementation or entry-level TTL setting; schedule as an optimization if expiry storms are observed.

## Invalidation

- Explicit eviction on writes via domain events.
- Time-based expiry via TTL.
- Full cache flush reserved for emergency runbook scenarios.

## Security

- Require TLS and AUTH for all connections.
- Store credentials in secret manager (Vault/AWS Secrets Manager).
- Enable ACLs limiting commands to necessary set (GET, SET, DEL, EXPIRE).

## Observability
- Cache metrics (Micrometer): enable cache metrics and export to Prometheus (hit/miss/puts/gets/evictions).
- Redis side: track latency, connected clients, memory usage via Redis Exporter.
- Alert on sustained miss spikes, rising evictions, or memory fragmentation.
- Alert on sustained miss spikes or memory fragmentation.

## Runbooks

- Provisioning documented in `docs/runbooks/cache.md` (added).
- Include disaster recovery (rebuild instructions) and failover procedures.
