# CDN Rollout Playbook (Draft)

## Provider
Cloudflare CDN + Workers for Phase 4 pilot.

## Objectives
- Serve static assets via Cloudflare edge.
- Apply API caching headers for read-heavy endpoints.
- Integrate rate limiting/WAF rules for zero-trust posture.

## Tasks
1. Configure Cloudflare zone + DNS (ensure split-horizon where needed).
2. Establish Cloudflare Tunnel / mTLS to Istio ingress.
3. Deploy Workers for header rewrites and cache invalidation hooks.
4. Monitor via Cloudflare analytics + Prometheus exporters.

## Rollback
- Disable cache rules and revert DNS to origin.
- Flush cached entries via Cloudflare API if stale data persists.

## References
- `docs/research/cdn-evaluation.md`
- Cloudflare Workers KV best practices
