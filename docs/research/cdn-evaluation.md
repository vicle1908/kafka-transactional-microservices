# CDN / Edge Caching Evaluation (2025-10-08)

## Candidates
- **Cloudflare CDN + Workers** – rich edge functions, global POP footprint, strong DDoS mitigation.
- **AWS CloudFront + Lambda@Edge** – native fit for AWS workloads, origin access control integration.
- **Fastly** – powerful edge configuration via VCL/Compute@Edge, premium cost.

## Selection Factors
1. Data locality & compliance requirements.
2. Integration with API gateway (Spring Cloud Gateway) and Istio ambient mesh.
3. Logging & metrics export to existing observability stack.
4. Cost & billing predictability.

## Recommendation
Adopt Cloudflare CDN for Phase 4 pilots (static assets + API caching headers) because:
- Easier zero-trust integration with mTLS mesh using Cloudflare Tunnels.
- Built-in rate limiting and WAF rules align with security requirements.
- Developer experience (Workers + KV) suits potential edge transformation use cases.

Document rollout plan in `docs/runbooks/cdn.md` when implementation begins.

## References
- Cloudflare Enterprise feature matrix (Sep 2025)
- Istio ambient mode + Cloudflare tunnel integration blog (Aug 2025)
- AWS CloudFront origin control docs (compared for parity)
