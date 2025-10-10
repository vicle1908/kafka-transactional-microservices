# Vault Runbook

## Deployment
- Deploy HashiCorp Vault in HA mode (Kubernetes or VM) with auto-unseal (e.g., HSM or KMS integration).
- Enable namespaces for environment separation (dev/staging/prod).
- Configure storage backend (e.g., Consul, Raft) and TLS certificates.

## Kubernetes Integration
- Install Vault Agent Injector for pod-based secret delivery.
- Define Vault roles and policies per service account (e.g., orders, payments).
- Annotate deployments to inject secrets via sidecar; mount shared volume for application access.

## Operations
- Monitor Vault health (`vault status`), seal status, and audit logs.
- Rotate root tokens; enforce short-lived client tokens via policies.
- Set up Prometheus metrics and alerts for seal state, auth failures, and request rates.

## Incident Response
- If Vault seals unexpectedly: follow auto-unseal troubleshooting, check storage backend.
- If injector fails: verify webhook service, certificates, and namespace annotations.
- For compromised credentials: revoke associated leases, rotate underlying credentials, audit access logs.

## References
- ADR 0004 – Secrets Management Strategy.
- HashiCorp Vault operations guide.
