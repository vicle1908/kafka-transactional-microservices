# ADR 0006: Adopt Istio for Service Mesh

## Status

Proposed

## Context

The project requires a robust solution for handling cross-cutting concerns in a microservices architecture, including service discovery, load balancing, traffic management, security, and observability. While libraries like Spring Cloud can handle some of these, a service mesh offers a more powerful, transparent, and language-agnostic approach by moving this logic out of the application and into the platform infrastructure.

The existing plan mentions Istio in Phase 1 for a preliminary setup, but a dedicated phase is needed to formalize its adoption and integrate it deeply into the project's workflows.

## Decision

We will adopt **Istio in ambient mode** as the official service mesh for this project. A new, dedicated phase (Phase 7) will be added to the implementation plan to cover its full integration.

The ambient mode is chosen for its reduced resource overhead (no sidecars per pod), simplified operations, and improved security posture (default L4 mTLS).

Key integration points will include:
1.  **Traffic Management**: Using Istio's `VirtualService` and `DestinationRule` resources for canary deployments, A/B testing, and fine-grained routing.
2.  **Security**: Enforcing strict mTLS for all service-to-service communication and implementing `AuthorizationPolicy` for least-privilege access control.
3.  **Ingress**: Utilizing the Kubernetes `Gateway` API for ingress traffic management.
4.  **Observability**: Integrating Istio's telemetry with the existing Prometheus and Grafana stack for comprehensive monitoring.

## Consequences

### Positive

*   **Simplified Application Code**: Microservices can offload complex concerns like retries, timeouts, and mTLS to the mesh, making the application code leaner and more focused on business logic.
*   **Enhanced Security**: Zero-trust security is enforced by default with automatic mTLS.
*   **Advanced Traffic Control**: Enables sophisticated deployment strategies like canary releases and A/B testing with minimal operational overhead.
*   **Improved Observability**: Provides deep, consistent visibility into service-to-service communication without requiring application-level instrumentation.

### Negative

*   **Increased Operational Complexity**: Managing a service mesh adds another layer to the infrastructure that requires expertise to operate and troubleshoot.
*   **Learning Curve**: The team will need to become proficient with Istio concepts and APIs.
*   **Resource Overhead**: While ambient mode is more efficient than traditional sidecars, the Istio control plane and waypoint proxies still consume cluster resources.
