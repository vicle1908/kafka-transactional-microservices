# Service Mesh Runbook (Istio Ambient)

## Scope
Guidance for deploying, operating, and troubleshooting Istio ambient mode across staging and production clusters.

## Deployment Steps
1. Apply Istio operator manifests via Terraform/Helm.
2. Enable ambient profile (ztunnel + waypoint) for target namespaces.
3. Configure Gateway API integration for Spring Cloud Gateway ingress.
4. Run smoke tests validating mTLS, routing, and telemetry.

## Routine Operations
- Daily checks: `istioctl proxy-status`, `kubectl get ztunnel`, `kubectl get waypoint`.
- Monitor certificate expiry via Prometheus alert rules.
- Validate mesh policies after namespace onboarding.

## Upgrades
- Review release notes (focus on ambient GA/bug fixes).
- Test upgrades in staging; use canary namespace before global rollout.
- Backup custom resources; run traffic/tracing regression tests post-upgrade.

## Incident Response
- Connectivity loss: verify ztunnel/waypoint pods, inspect network policies, check control plane health.
- mTLS failures: confirm certificates, trust domain alignment, and destination rules.
- Performance issues: analyze Envoy metrics, increase waypoint replicas, adjust traffic policies.

## Integration Points
- Spring Cloud Gateway (north-south ingress).
- Prometheus/Grafana dashboards for mesh telemetry.
- Jaeger/Tempo tracing pipelines.

## References
- Istio ambient mode documentation (1.24+).
- Internal diagrams (`docs/architecture/service-mesh.png`).
