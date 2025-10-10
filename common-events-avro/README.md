# common-events-avro

Shared Apache Avro schemas that define the canonical event contracts published via the transactional outbox. Schemas generated from this module are used by Debezium connectors and Spring Kafka consumers.

## Build

```bash
./gradlew :common-events-avro:build
```

Generated sources are emitted under `build/generated-main-avro-java` by default. Downstream modules can depend on this project to use the generated classes or consume the `.avsc` files when publishing to the Schema Registry.
