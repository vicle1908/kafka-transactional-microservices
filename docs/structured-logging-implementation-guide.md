# Structured Logging Implementation Guide

## Overview

This guide explains how to implement structured logging in microservices using the `StructuredLogger` utility provided in the `common-observability` module.

## Prerequisites

1. The `common-observability` module with the `StructuredLogger` class
2. Jackson dependencies for JSON serialization

## Implementation Steps

### 1. Add Dependencies

Ensure your service's `build.gradle.kts` includes the `common-observability` module:

```kotlin
dependencies {
    implementation(project(":common-observability"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
```

### 2. Use StructuredLogger in Your Services

Import and use the `StructuredLogger` in your service classes:

```kotlin
import com.example.observability.StructuredLogger
import org.springframework.stereotype.Service

@Service
class OrderService {
    private val logger = StructuredLogger.getLogger(OrderService::class.java)
    
    fun createOrder(orderId: String, customerId: String, items: List<String>): String {
        logger.info("Creating new order", 
                   "orderId" to orderId, 
                   "customerId" to customerId, 
                   "itemCount" to items.size)
        
        try {
            // Order processing logic
            logger.info("Order created successfully", 
                       "orderId" to orderId, 
                       "customerId" to customerId)
            return orderId
        } catch (e: Exception) {
            logger.error("Failed to create order", 
                        "orderId" to orderId, 
                        "customerId" to customerId, 
                        "errorMessage" to e.message)
            throw e
        }
    }
}
```

### 3. Log Message Structure

The `StructuredLogger` generates JSON-formatted log messages with the following structure:

```json
{
  "timestamp": "2023-01-01T10:00:00.000Z",
  "message": "Creating new order",
  "orderId": "order-123",
  "customerId": "customer-456",
  "itemCount": 3
}
```

This structure makes it easier for Logstash and Elasticsearch to parse and index the logs.

### 4. Best Practices

1. Always include identifiers like `orderId`, `customerId` in log messages for correlation
2. Use appropriate log levels:
   - `DEBUG` for detailed diagnostic information
   - `INFO` for general operational messages
   - `WARN` for potentially harmful situations
   - `ERROR` for error events that might still allow the application to continue running
3. Include contextual information that helps with debugging and monitoring
4. Avoid logging sensitive information like passwords or credit card numbers

## Integration with ELK Stack

With structured logging implemented:

1. Filebeat will collect logs from service containers
2. Logstash will parse the JSON log messages
3. Elasticsearch will index the structured data
4. Kibana will visualize the log data

This enables powerful searching, filtering, and dashboard creation capabilities.
