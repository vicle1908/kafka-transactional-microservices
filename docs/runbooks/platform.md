# Platform Runbook

## Related Runbooks

- [CI/CD Operations](./cicd-operations.md) - GitHub Actions workflows, build, test, and deployment operations
- [Service Mesh](./service-mesh.md) - Istio configuration and traffic management
- [Debezium](./debezium.md) - CDC connector operations and monitoring

## Local Environment

- Use `infra/compose.yml` to spin up Kafka 4.1.0, Schema Registry, Debezium Connect, PostgreSQL, and AKHQ.

  ```bash
  docker compose -f infra/compose.yml up -d
  ```

- Credentials: PostgreSQL `app/app`; Schema Registry exposed on `http://localhost:8081`.

## Shared Clusters

- Provision via Terraform modules (pending) with Kafka KRaft brokers and Schema Registry.
- Apply broker configuration from `infra/kafka/values.yaml`.

## Observability

- Access AKHQ at `http://localhost:8080` for topic management.
- Integrate Prometheus exporters for Kafka and Connect in future phases.

## Troubleshooting

- Kafka logs: `docker logs kafka`.
- Debezium connect health: `curl http://localhost:8083/connectors`.
- Postgres access: `psql -h localhost -U app orders`.
