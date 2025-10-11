# ADR 0002 – Saga Coordination Approach

## Status

Accepted – 2025-10-07

## Context

We need to coordinate cross-service workflows (Order → Payment → Inventory → Notification). Options:

1. Pure choreography with domain events.
2. Central orchestration engine (Temporal, Camunda, etc.).

## Decision

Use Temporal for complex sagas requiring retries, compensations, and visibility, while retaining event-choreographed sagas for simple, two-step interactions. Temporal workers will manage activities per domain service, persisting saga state externally. Choreographed flows remain for lightweight operations to avoid over-coordination.

## Consequences

- Provides durable workflow execution, centralized logging, and simplified compensation logic.
- Introduces operational overhead (Temporal cluster or managed service) but improves resilience for business-critical flows.
- Requires developer training on Temporal workflow patterns and shared libraries.
