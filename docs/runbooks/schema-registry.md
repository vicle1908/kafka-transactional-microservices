# Schema Registry Operations

This document describes the procedures for working with the Schema Registry, including publishing schemas, compatibility checking, and management operations.

## Schema Registry Overview

The Schema Registry provides a centralized store for Avro schemas used in Kafka messaging. It ensures schema compatibility and enables schema evolution while maintaining backward compatibility.

### Key Concepts

1. **Subject**: A named entity that holds a set of schema versions (e.g., `orders-value`, `payments-value`)
2. **Schema**: An Avro schema definition that describes the structure of messages
3. **Version**: A specific iteration of a schema within a subject
4. **Compatibility**: Rules that govern how schemas can evolve (BACKWARD, FORWARD, FULL, etc.)

## Publishing Schemas

### Using the Script

The `scripts/schema-publish.sh` script provides a convenient way to publish schemas:

```bash
# Publish a single schema
./scripts/schema-publish.sh --subject orders-value --schema common-events-avro/src/main/avro/OrderEvent.avsc

# List all registered subjects
./scripts/schema-publish.sh --list-subjects

# List versions for a subject
./scripts/schema-publish.sh --list-versions orders-value
```

### Manual Publishing

To publish schemas manually, use the Schema Registry REST API:

```bash
# Register a new schema version
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"OrderEvent\",...}"}' \
  http://localhost:8081/subjects/orders-value/versions

# Get the latest schema for a subject
curl -X GET http://localhost:8081/subjects/orders-value/versions/latest
```

## Schema Compatibility

### Compatibility Modes

1. **BACKWARD** (default): New schema can read data written by old schema
2. **FORWARD**: Old schema can read data written by new schema
3. **FULL**: Both BACKWARD and FORWARD compatibility
4. **NONE**: No compatibility checking

### Checking Compatibility

Before publishing, schemas are checked for compatibility:

```bash
# Check compatibility for a subject
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"OrderEvent\",...}"}' \
  http://localhost:8081/compatibility/subjects/orders-value/versions/latest
```

### Compatibility Rules

- Fields can be added with default values
- Fields can be removed if they had default values
- Field types cannot be changed
- Field names cannot be changed
- Enum symbols can be added but not removed

## Schema Evolution Best Practices

### Adding Fields

Always provide default values when adding new fields:

```json
{
  "type": "record",
  "name": "OrderEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "totalAmount", "type": "double", "default": 0.0}
  ]
}
```

### Removing Fields

Only remove fields that had default values:

```json
{
  "type": "record",
  "name": "OrderEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "status", "type": "string"}
    // Removed totalAmount field that had a default value
  ]
}
```

### Changing Field Types

Field types should generally not be changed. If necessary, create a new subject:

```bash
# Instead of changing field type, create a new subject
./scripts/schema-publish.sh --subject orders-v2-value --schema schemas/OrderEventV2.avsc
```

## Schema Management Procedures

### CI/CD Integration

Schema compatibility checking is integrated into the CI/CD pipeline:

1. During build, export Avro schemas: `./gradlew :common-events-avro:build`
2. Check compatibility: `./gradlew schemaCompatibilityCheck`
3. If compatible, proceed with deployment
4. If not compatible, fail the build and require schema review

### Schema Versioning

Follow semantic versioning for schemas:

- Major version changes for breaking schema changes
- Minor version changes for backward-compatible additions
- Patch version changes for non-breaking fixes

### Schema Deprecation

To deprecate a schema:

1. Mark the schema as deprecated in documentation
2. Announce deprecation to consumers
3. Provide migration path to new schema
4. Remove schema after grace period

## Troubleshooting

### Common Issues

1. **Compatibility Errors**
   - Check that new fields have default values
   - Verify that removed fields had default values
   - Ensure field types haven't changed

2. **Schema Registration Failures**
   - Verify Schema Registry is running
   - Check schema syntax and validity
   - Ensure subject name follows conventions

3. **Serialization Errors**
   - Verify that producer uses correct schema
   - Check that consumer can handle schema versions
   - Validate schema evolution rules

### Diagnostic Commands

```bash
# List all subjects
curl -X GET http://localhost:8081/subjects

# Get schema by ID
curl -X GET http://localhost:8081/schemas/ids/1

# Get schema versions for a subject
curl -X GET http://localhost:8081/subjects/orders-value/versions

# Get specific schema version
curl -X GET http://localhost:8081/subjects/orders-value/versions/1
```

## Security Considerations

### Authentication

Schema Registry should be secured with authentication:

- Use API keys for client authentication
- Configure SSL/TLS for encrypted communication
- Restrict access based on subject naming conventions

### Authorization

Implement role-based access control:

- Publishers can register new schemas
- Consumers can only read schemas
- Administrators can configure compatibility settings

## Monitoring and Alerting

### Key Metrics

1. **Schema Registration Rate**: Number of schemas registered per minute
2. **Compatibility Check Failures**: Number of failed compatibility checks
3. **Schema Registry Latency**: Response time for Schema Registry operations
4. **Storage Usage**: Disk space used by schema storage

### Alerting Thresholds

- **High Registration Rate**: Alert on unusually high schema registration rates
- **Compatibility Failures**: Alert immediately on compatibility check failures
- **Registry Unavailable**: Alert when Schema Registry becomes unreachable
- **Storage Low**: Alert when storage usage exceeds 80%

## Best Practices

### Schema Design

- Use descriptive field names
- Provide meaningful documentation in schemas
- Use appropriate data types (avoid generic types when specific types are available)
- Consider future evolution when designing schemas

### Version Management

- Keep schemas in version control
- Tag schema versions with release tags
- Document schema changes in release notes
- Use semantic versioning for schema evolution

### Testing

- Test schema compatibility before publishing
- Validate schema evolution with sample data
- Test consumer compatibility with new schemas
- Include schema tests in integration test suite
