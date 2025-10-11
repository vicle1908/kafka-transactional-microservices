# Shared Kafka Cluster Provisioning Plan

1. Provision three-node Kafka 4.1.0 KRaft cluster via Terraform module (pending repository).
2. Configure node IDs, listeners, and quorum voters per `infra/kafka/values.yaml`.
3. Create TLS certificates and ACLs for service accounts (orders, payments, inventory, notification, debezium, akhq).
4. Deploy Schema Registry (7.7.0) and AKHQ in the same namespace; restrict access via OAuth.
5. Output bootstrap servers, schema registry URL, and credentials to secret manager.
6. Document runbook steps for scaling and rotating credentials.

---

## CI/CD – GitHub Workflows Setup (2025-10-11)

- Hardened CI:
  - Added runner hardening step (egress audit) to CI
  - Made Gradle wrapper validation fail fast (no fallback)
  - Removed step-level timeouts; rely on job-level timeout-minutes
- Security:
  - CodeQL workflow granted security-events: write to upload results
  - Dependency Review now runs on pull_request with fail-on-severity: high (PR gate)
- Container Publish:
  - Fixed artifact upload path; dynamic JAR_FILE detection via GITHUB_ENV
  - Corrected Gradle cache key to use gradle/libs.versions.toml
- Load Test:
  - Aligned Gradle setup action to v4
  - Fixed Spring env var name SPRING_KAFKA_BOOTSTRAP_SERVERS in compose override

These changes align with WARP/AGENTS.md baselines and branch protection rules (merge only on green).
