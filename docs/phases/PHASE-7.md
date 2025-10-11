# Phase 7 – Service Mesh Integration (Istio)

## Architecture & Strategy

This phase implements a **hybrid gateway architecture**. The model is a deliberate choice to leverage the distinct strengths of two separate layers:

1. **Tier 1: Spring Cloud Gateway (The Application-Aware Edge)**: Continues to serve as the primary entry point for external (north-south) traffic. Its responsibility is to handle application-level concerns that require business context, such as complex user authentication (OIDC/OAuth2), API composition (BFF patterns), and request/response transformations.
2. **Tier 2: Istio Service Mesh (The Network Infrastructure)**: Manages all internal service-to-service (east-west) traffic after it has been routed from the Spring Cloud Gateway. Its responsibility is to handle network-level concerns transparently, including enforcing encryption (mTLS), managing retries/timeouts, and executing fine-grained traffic shifting for canary releases.

This separation of concerns allows developers to continue using the familiar Spring ecosystem for complex edge logic while gaining the security and resilience benefits of a service mesh for internal communication.

## Objectives

- Integrate Istio ambient mode as the service mesh for all east-west traffic, operating in a **hybrid model** with the existing Spring Cloud Gateway.
- Utilize Istio for advanced traffic management and resilience for internal service-to-service communication.
- Enforce a zero-trust security model within the mesh using Istio's security features.
- Ensure seamless observability by integrating Istio telemetry with the project's existing monitoring stack.

## Deliverables

- An ADR (`docs/adrs/0006-istio-adoption.md`) formalizing the adoption of Istio in a hybrid model.
- Production-ready Infrastructure-as-Code (Helm/Terraform) for deploying and managing Istio's ambient profile.
- Istio `Gateway` and `VirtualService` resources configured to accept and route traffic from Spring Cloud Gateway into the service mesh.
- A global `PeerAuthentication` policy enforcing strict mTLS for all internal traffic.
- Granular `AuthorizationPolicy` resources for least-privilege service-to-service access.
- An updated `canary-deployment.yml` workflow that uses Istio for traffic splitting on internal services.
- Dedicated Grafana dashboards for monitoring service mesh health and performance.
- Updated runbook (`docs/runbooks/service-mesh.md`) documenting the hybrid traffic flow and context propagation.

## Task Board

| ID    | Task                                                                      | Owner              | Status                         | Notes                                                                                                                 |
|-------|---------------------------------------------------------------------------|--------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| P7.1  | Author ADR 0006 for Istio Adoption                                        | Architecture Team  | Completed                      | See `docs/adrs/0006-istio-adoption.md`.                                                                               |
| P7.2  | Develop IaC scripts (Helm/Terraform) for Istio ambient profile deployment | Platform Team      | Scheduled (due 2025-10-14)     | Execute AGENTS research workflow (Context7, DeepWiki) before drafting modules.                                        |
| P7.3  | Configure Istio to receive traffic from Spring Cloud Gateway              | Platform Team      | Scheduled (due 2025-10-14)     | Define `Gateway` and `VirtualService` to route traffic from SCG to internal services via waypoint proxies.            |
| P7.4  | Define `VirtualService` and `DestinationRule` for internal services       | Service Owners     | Scheduled (kickoff 2025-10-15) | Align policies with security review and platform templates.                                                           |
| P7.5  | Implement global `PeerAuthentication` policy for strict mTLS              | Security Team      | Scheduled (due 2025-10-16)     | Applies to all traffic within the mesh.                                                                               |
| P7.6  | Define `AuthorizationPolicy` for service-to-service communication         | Security Team      | Scheduled (due 2025-10-16)     | Enforce least-privilege access between services; document matrices.                                                   |
| P7.7  | Update `canary-deployment.yml` to use Istio traffic shifting              | DevOps Team        | Scheduled (due 2025-10-17)     | For internal service canaries; ensure CI smoke tests cover mesh path.                                                 |
| P7.8  | Create Grafana dashboard for Istio metrics                                | Platform Team      | Scheduled (due 2025-10-18)     | Leverage existing observability modules and OpenTelemetry collector.                                                  |
| P7.9  | Run integration tests to validate mTLS, routing, and latency              | QA Team            | Scheduled (due 2025-10-20)     | Include failure injection, fallback validation, and **latency benchmarking** to measure overhead of the hybrid model. |
| P7.10 | Validate and document trace context propagation                           | Observability Team | Scheduled (due 2025-10-18)     | Ensure W3C/B3 trace headers flow correctly from SCG through the Istio mesh to provide end-to-end traces.              |
| P7.11 | Update `service-mesh.md` runbook with hybrid architecture details         | Documentation Team | Scheduled (due 2025-10-18)     | Incorporate research artifacts, new operational steps, and the **GitOps strategy** for managing configurations.       |

## Execution Timeline & Checkpoints

| Date (2025) | Milestone                                                                                              | Dependencies                                | Exit Criteria                                                              |
|-------------|--------------------------------------------------------------------------------------------------------|---------------------------------------------|----------------------------------------------------------------------------|
| Oct 13-14   | Platform team completes research loop and drafts Helm/Terraform ambient modules (`P7.2`, `P7.3`)       | Context7/DeepWiki research notes, ADR 0006  | Draft IaC committed to feature branch, design review scheduled             |
| Oct 15-16   | Security finalizes mesh security policies (`P7.5`, `P7.6`) and aligns `P7.4` service routing templates | Platform IaC drafts, service traffic matrix | Policy manifests pass lint/tests; ADR addendum PR opened                   |
| Oct 17      | DevOps updates canary workflow to leverage Istio traffic shifting (`P7.7`)                             | Mesh gateways configured in staging         | CI dry-run demonstrates traffic split, smoke tests green                   |
| Oct 18      | Observability delivers dashboards and trace propagation validation (`P7.8`, `P7.10`, `P7.11`)          | Canary workflow, mesh telemetry endpoints   | Grafana dashboard published; runbook updated with screenshots + procedures |
| Oct 20      | QA executes hybrid-routing integration suite (`P7.9`)                                                  | All prior milestones, updated runbook       | Test report filed under PHASE-7 board with pass/fail & defect list         |

## Research & Preparation Checklist

- ✅ Confirm ADR 0006 scope and highlight waypoint strategy decision waiting on Platform/Security consensus.
- ☐ Run `AGENTS.md` research workflow (Context7 library docs, DeepWiki repo search, existing IaC examples) **before** authoring Helm/Terraform modules.
- ☐ Capture research artifacts, spike outcomes, and policy matrices in `docs/phases/PHASE-7.md` alongside task updates.
- ☐ Cross-check Istio module versions against `docs/version-matrix.md`; update catalog entries if upgrades are needed.
- ☐ Coordinate with CI owners to provision mesh components in non-prod environments to support `P7.7` smoke tests.

## Definition of Done

1. Ambient profile IaC merged with platform review sign-off and reproducible deployment instructions.
2. Spring Cloud Gateway ingress integrated with Istio waypoints, including zero-trust policies and documented service-to-service matrices.
3. Mesh-aware canary workflow running in CI with traceable smoke test evidence.
4. Grafana dashboards and OpenTelemetry traces confirming SCG→Istio→service continuity, referenced in `docs/runbooks/service-mesh.md`.
5. QA integration report demonstrating mTLS enforcement, latency targets within agreed thresholds, and recovery from injected failures.

## Risks & Mitigations

- **Increased Operational Complexity**: The hybrid model requires managing two gateway technologies.
  - **Mitigation**: Manage both Spring Cloud Gateway and Istio configurations in the same Git repository (GitOps). This allows changes to be reviewed and deployed atomically. Document this process and clear naming conventions in the runbook (**Task P7.11**).

- **Latency Overhead**: The "double hop" through both gateways will introduce latency.
  - **Mitigation**: This overhead must be quantified. The load tests conducted in Phase 6 and the integration tests in this phase (**Task P7.9**) must benchmark performance against a baseline to ensure the overhead is within acceptable limits.

- **Observability Gaps**: End-to-end traces could break at the boundary between Spring Cloud Gateway and the Istio mesh.
  - **Mitigation**: This is a critical integration point that requires explicit configuration and verification. **Task P7.10** is dedicated to ensuring W3C/B3 trace headers are correctly propagated and that traces appear unified in the observability platform.
