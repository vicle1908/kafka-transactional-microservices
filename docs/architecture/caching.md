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

## Invalidation
- Explicit eviction on writes via domain events.
- Time-based expiry via TTL.
- Full cache flush reserved for emergency runbook scenarios.

## Security
- Require TLS and AUTH for all connections.
- Store credentials in secret manager (Vault/AWS Secrets Manager).
- Enable ACLs limiting commands to necessary set (GET, SET, DEL, EXPIRE).

## Observability
- Track hit/miss ratio, latency, memory usage via Redis Exporter + Prometheus.
- Alert on sustained miss spikes or memory fragmentation.

## Runbooks
- Provisioning documented in `docs/runbooks/cache.md`.
- Include disaster recovery (rebuild instructions) and failover procedures.
