# CDN Rollout Playbook

## Provider
Cloudflare CDN + Workers for Phase 4 pilot.

## Objectives
- Serve static assets via Cloudflare edge.
- Apply API caching headers for read-heavy endpoints.
- Integrate rate limiting/WAF rules for zero-trust posture.
- Monitor CDN performance and edge metrics.

## CDN Metrics and Monitoring

### Key Metrics to Monitor

1. **Performance Metrics**:
   - Cache hit ratio
   - Response time percentiles
   - Bandwidth usage
   - Request rate

2. **Availability Metrics**:
   - Uptime
   - Error rates (4xx, 5xx)
   - Origin reachability

3. **Security Metrics**:
   - WAF blocked requests
   - DDoS mitigation events
   - Bot detection statistics

### Grafana Dashboard

Create a Grafana dashboard with panels for:
- Cache hit ratio over time
- Response time distribution
- Bandwidth usage by region
- Error rate trends
- WAF blocked requests by rule
- Top requested resources

### Alerting Rules

Configure alerts for:
- Low cache hit ratio (< 80%)
- High error rates (> 5%)
- Increased response times (> 2x baseline)
- DDoS attack detection
- Origin server failures

## Implementation Tasks

1. Configure Cloudflare zone + DNS (ensure split-horizon where needed).
2. Establish Cloudflare Tunnel / mTLS to Istio ingress.
3. Deploy Workers for header rewrites and cache invalidation hooks.
4. Configure caching rules for static assets and API responses.
5. Set up monitoring and alerting for CDN metrics.
6. Implement WAF rules and rate limiting policies.

## Rollback
- Disable cache rules and revert DNS to origin.
- Flush cached entries via Cloudflare API if stale data persists.
- Revert WAF and rate limiting rules if causing issues.

## References
- `docs/research/cdn-evaluation.md`
- Cloudflare Workers KV best practices
- Cloudflare Analytics documentation
- Grafana dashboard examples for CDN metrics