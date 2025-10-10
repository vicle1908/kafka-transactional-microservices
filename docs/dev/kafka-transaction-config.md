# Kafka Transaction Configuration Guide

## Overview

This guide explains how to properly configure Kafka transaction management in Spring Boot microservices to ensure exactly-once semantics (EOS) when working with database transactions and Kafka message production.

## Transaction Manager Configuration

To enable exactly-once semantics, services must configure a `KafkaTransactionManager` that coordinates database transactions with Kafka operations:

```kotlin
@Bean
fun kafkaTransactionManager(
    producerFactory: ProducerFactory<String, String>
): KafkaTransactionManager<String, String> = KafkaTransactionManager(producerFactory)
```

## Listener Container Factory Configuration

When configuring the `ConcurrentKafkaListenerContainerFactory`, it's important to properly set the transaction manager. In Spring Kafka 3.2+, the correct approach is to use the `containerProperties.kafkaAwareTransactionManager` property:

```kotlin
@Bean
fun kafkaListenerContainerFactory(
    configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
    consumerFactory: ConsumerFactory<String, String>,
    transactionManager: KafkaAwareTransactionManager<String, String>
): ConcurrentKafkaListenerContainerFactory<String, String> {
    val factory = ConcurrentKafkaListenerContainerFactory<String, String>().also {
        configurer.configure(
            it as ConcurrentKafkaListenerContainerFactory<Any, Any>,
            consumerFactory as ConsumerFactory<Any, Any>
        )
    }
    factory.containerProperties.kafkaAwareTransactionManager = transactionManager
    factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
    return factory
}
```

### Important Note on Deprecated Properties

In previous versions, it was common to configure the transaction manager like this:

```kotlin
// DEPRECATED - Do not use this approach
factory.transactionManager = transactionManager
```

This approach is deprecated in Spring Kafka 3.2+ and will cause runtime errors. Always use `containerProperties.kafkaAwareTransactionManager` instead.

## Exactly-Once Semantics (EOS) Configuration

To enable exactly-once semantics, configure the following Kafka producer properties:

```properties
# Enable idempotent producer
enable.idempotence=true
acks=all
retries=Integer.MAX_VALUE
max.in.flight.requests.per.connection=5

# Transactional producer settings
transactional.id=your-transactional-id
```

And configure consumers with:

```properties
isolation.level=read_committed
```

## Testing Transactional Consumers

When testing transactional Kafka consumers, use Testcontainers with Embedded Kafka to verify exactly-once behavior:

```kotlin
@Test
fun `should process messages exactly once`() {
    // Send duplicate messages
    // Verify only processed once using processed_events ledger
}
```

## Common Issues and Solutions

### NoSuchMethodError with transactionManager

If you encounter a `NoSuchMethodError` related to `transactionManager`, check that you're using the correct property:

```
// Incorrect in Spring Kafka 3.2+
factory.transactionManager = transactionManager

// Correct approach
factory.containerProperties.kafkaAwareTransactionManager = transactionManager
```

### Transaction Synchronization Issues

Ensure that all database operations and Kafka message production happen within the same transactional context by using the `@Transactional` annotation on your service methods.

## References

- [Spring for Apache Kafka Documentation](https://docs.spring.io/spring-kafka/reference/index.html)
- [Apache Kafka Documentation on Transactions](https://kafka.apache.org/documentation/#transactions)
- ADR 0001 on Transactional Outbox Pattern