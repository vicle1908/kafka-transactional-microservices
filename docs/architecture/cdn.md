# CDN & Edge Strategy

## Objectives
- Reduce latency for static assets and cacheable API responses.
- Protect origin services from traffic spikes via edge caching and rate limiting.
- Enable blue/green or canary routing at the edge where appropriate.

## Provider Evaluation (Phase 3 Task)
- Compare Cloudflare, AWS CloudFront, and Fastly for pricing, regional coverage, and integration.
- Consider managed WAF/DDoS protections and Terraform support.

## Configuration Guidelines
- Use cache keys combining host + path + critical query parameters.
- Normalize headers to avoid cache fragmentation (strip volatile headers).
- Set `Cache-Control` policies per route (e.g., `public, max-age=900` for static assets, `stale-while-revalidate` for semi-dynamic data).
- Configure edge functions/workers for custom authentication or token refresh when needed.

## Invalidation & Deployment
- Automate cache invalidation via CI/CD when static artifacts change.
- Provide manual purge runbooks with safety checks.

## Observability
- Collect metrics: cache hit ratio, origin error rate, latency percentiles.
- Integrate with Grafana dashboards and alert when hit ratio drops below thresholds or origin fetches spike.

## Security
- Enforce TLS 1.2+ between clients and edge; TLS from edge to origin with mutual authentication if supported.
- Enable bot protection/WAF rules as part of baseline config.

## Runbooks
- Document CDN rollout, configuration changes, and purge procedures in `docs/runbooks/cdn.md`.
