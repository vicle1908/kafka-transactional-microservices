# ADR 0004 – Secrets Management Strategy

## Status
Accepted – 2025-10-07

## Context
Microservices require secure storage and distribution of credentials (database passwords, API keys, certificates). Options considered:
1. Kubernetes Secrets with static values.
2. HashiCorp Vault (agent injector or secrets operator).
3. Cloud provider secret stores (AWS Secrets Manager, GCP Secret Manager) integrated via sidecars.

## Decision
Use HashiCorp Vault as the primary secrets manager. Kubernetes workloads will retrieve secrets via Vault Agent Injector, writing short-lived credentials to shared volumes. For services running outside Kubernetes, use Vault’s API. Cloud-native secret stores remain options for specialized deployments but Vault provides the consistent control plane we need.

## Consequences
- Provides centralized audit, policy enforcement, dynamic secrets, and rotation.
- Requires Vault deployment/operations and injector configuration.
- Application pods must mount injected secrets and reload when updated.
