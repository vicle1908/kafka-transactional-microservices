# Shared Kafka Cluster Provisioning Plan

1. Provision three-node Kafka 4.1.0 KRaft cluster via Terraform module (pending repository).
2. Configure node IDs, listeners, and quorum voters per `infra/kafka/values.yaml`.
3. Create TLS certificates and ACLs for service accounts (orders, payments, inventory, notification, debezium, akhq).
4. Deploy Schema Registry (7.7.0) and AKHQ in the same namespace; restrict access via OAuth.
5. Output bootstrap servers, schema registry URL, and credentials to secret manager.
6. Document runbook steps for scaling and rotating credentials.
