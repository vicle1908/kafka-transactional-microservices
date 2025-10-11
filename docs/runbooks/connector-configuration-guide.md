# Debezium Connector Configuration Guide

## Overview

This document explains the different Debezium connector configurations available in the project and when to use each one.

## Connector Types

### Generic Configuration (`infra/debezium/outbox-connector.json`)

This configuration is a template that can be used as a starting point for any service. It uses the `BinaryDataConverter` with Avro serialization, which is the recommended approach for production deployments.

**Key Features:**

- Uses `io.debezium.converters.spi.BinaryDataConverter` with Avro delegate
- Direct Avro serialization without intermediate JSON conversion
- Better performance for high-throughput scenarios
- More compact message format

**When to Use:**

- For services that require high throughput
- When you want to use the latest Debezium features
- As a template for creating service-specific configurations

### Service-Specific Configurations (`infra/debezium/connectors/*.json`)

These configurations are tailored for specific services (orders, payments, inventory). They use `StringConverter` for keys and `AvroConverter` for values.

**Key Features:**

- Uses `org.apache.kafka.connect.storage.StringConverter` for keys
- Uses `io.confluent.connect.avro.AvroConverter` for values
- More explicit field mapping in the Outbox Event Router configuration
- Includes internal converter configurations for Connect

**When to Use:**

- For specific services in production
- When you need more control over field mappings
- When following the exact patterns established in the project

## Configuration Comparison

| Feature | Generic Config | Service-Specific Config |
|--------|----------------|-------------------------|
| Key Converter | BinaryDataConverter | StringConverter |
| Value Converter | BinaryDataConverter | AvroConverter |
| Schema Registry | Yes | Yes |
| Field Mapping | Basic | Detailed |
| Performance | Higher | Standard |
| Complexity | Lower | Higher |

## Field Mapping Differences

### Generic Configuration

```json
"transforms.outbox.table.fields.additional.placement": "headers:header,aggregateId:header"
```

### Service-Specific Configuration

```json
"transforms.outbox.table.fields.additional.placement": "id:envelope:eventId,aggregate_id:envelope:aggregateId,aggregate_type:envelope:aggregateType,event_type:envelope:eventType,payload:envelope:payload,headers:envelope:headers"
```

## Recommendations

1. **For High-Throughput Services**: Use the generic configuration as a template and customize as needed.

2. **For Standard Services**: Use the service-specific configurations as they are already tuned for the specific services.

3. **For New Services**: Start with a service-specific configuration and modify according to your needs.

4. **For Development/Testing**: Either configuration works, but the service-specific ones are more representative of production.

## Migration Path

If you're migrating from one configuration to another:

1. **From Service-Specific to Generic**:
   - Update the converter configurations
   - Simplify the field mappings
   - Test thoroughly to ensure message format compatibility

2. **From Generic to Service-Specific**:
   - Add detailed field mappings
   - Update converter configurations
   - Verify schema registry integration

## Best Practices

1. Always test connector configurations in a staging environment before deploying to production.

2. Monitor connector lag and performance metrics after any configuration changes.

3. Keep connector configurations in version control and document any changes.

4. Use the same schema registry URL across all connectors for consistency.

5. Ensure database credentials are properly secured and rotated regularly.

## Troubleshooting

### Common Issues

1. **Schema Registry Connection Issues**:
   - Verify the schema registry URL is correct
   - Check network connectivity
   - Ensure credentials are valid

2. **Converter Errors**:
   - Verify the converter class names are correct
   - Check that required JAR files are available
   - Ensure schema registry integration is properly configured

3. **Field Mapping Issues**:
   - Verify that the field names match the database schema
   - Check that the placement directives are correct
   - Ensure the routing configuration matches your topic naming strategy

## References

- Debezium Outbox Event Router Documentation
- Confluent Schema Registry Documentation
- Kafka Connect Documentation
- Project ADRs on outbox pattern implementation
