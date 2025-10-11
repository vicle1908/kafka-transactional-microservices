# Common Outbox Relay Module

This module provides a fallback mechanism for the transactional outbox pattern when Debezium CDC is not available or cannot be used. It implements a polling relay that periodically checks for pending outbox messages and publishes them to Kafka.

## Overview

The transactional outbox pattern ensures that domain data changes and event publications happen atomically within the same database transaction. While Debezium with the Outbox Event Router SMT is the preferred mechanism for this pattern, this module provides a polling-based fallback for environments where CDC is not available.

## Components

### OutboxMessage Entity

The `OutboxMessage` entity represents an event that needs to be published to Kafka:

```kotlin
@Entity
@Table(name = "outbox")
open class OutboxMessage(
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "headers", columnDefinition = "TEXT")
    val headers: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set
}
```

### OutboxStatus Enum

The `OutboxStatus` enum tracks the state of each outbox message:

```kotlin
enum class OutboxStatus {
    PENDING,  // Message is waiting to be processed
    SENT,     // Message has been successfully sent to Kafka
    FAILED    // Message failed to be sent to Kafka
}
```

### OutboxRepository

The `OutboxRepository` provides methods for querying and updating outbox messages:

```kotlin
@Repository
interface OutboxRepository : JpaRepository<OutboxMessage, UUID> {
    @Query("SELECT o FROM OutboxMessage o WHERE o.status = :status ORDER BY o.occurredAt ASC")
    fun findByStatusOrderByOccurredAtAsc(
        @Param("status") status: OutboxStatus,
        pageable: Pageable,
    ): List<OutboxMessage>

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus WHERE o.id IN :ids")
    fun updateStatusForIds(
        @Param("ids") ids: List<UUID>,
        @Param("newStatus") newStatus: OutboxStatus,
    )

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus, o.publishedAt = :publishedAt WHERE o.id = :id")
    fun markAsSent(
        @Param("id") id: UUID,
        @Param("newStatus") newStatus: OutboxStatus,
        @Param("publishedAt") publishedAt: Instant,
    )

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus WHERE o.id = :id")
    fun markAsFailed(
        @Param("id") id: UUID,
        @Param("newStatus") newStatus: OutboxStatus,
    )

    @Query("SELECT COUNT(o) FROM OutboxMessage o WHERE o.status = :status")
    fun countByStatus(
        @Param("status") status: OutboxStatus,
    ): Long

    @Query("SELECT o FROM OutboxMessage o WHERE o.occurredAt BETWEEN :startTime AND :endTime ORDER BY o.occurredAt ASC")
    fun findByOccurredAtBetweenOrderByOccurredAtAsc(
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant,
        pageable: Pageable,
    ): List<OutboxMessage>
}
```

### OutboxRelayService

The `OutboxRelayService` is the core service that processes outbox messages:

```kotlin
@Service
class OutboxRelayService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val transactionTemplate: TransactionTemplate,
    private val kafkaTransactionManager: KafkaTransactionManager<String, Any>,
    private val metricsService: OutboxMetricsService,
) {
    private val batchSize = 100

    @Transactional
    fun processPendingMessages(): Int {
        // Implementation details...
    }

    @Transactional
    fun processMessagesByTimeRange(
        startTime: Instant,
        endTime: Instant,
    ): Int {
        // Implementation details...
    }

    @Transactional
    fun replayMessage(messageId: String): Boolean {
        // Implementation details...
    }

    fun getPendingMessageCount(): Long {
        // Implementation details...
    }

    private fun processMessage(message: OutboxMessage) {
        // Implementation details...
    }

    private fun determineTopic(message: OutboxMessage): String {
        // Implementation details...
    }

    private fun extractKey(message: OutboxMessage): String? {
        // Implementation details...
    }

    private fun convertPayload(message: OutboxMessage): Any {
        // Implementation details...
    }

    private fun markMessageAsSent(message: OutboxMessage) {
        // Implementation details...
    }

    private fun markMessageAsFailed(message: OutboxMessage) {
        // Implementation details...
    }
}
```

### ScheduledOutboxProcessor

The `ScheduledOutboxProcessor` runs periodically to process pending outbox messages:

```kotlin
@Component
class ScheduledOutboxProcessor(
    private val outboxRelayService: OutboxRelayService,
    meterRegistry: MeterRegistry,
) {
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    fun processPendingMessages() {
        // Implementation details...
    }
}
```

### OutboxController

The `OutboxController` provides REST endpoints for manual processing and replay:

```kotlin
@RestController
@RequestMapping("/api/outbox")
class OutboxController(
    private val outboxRelayService: OutboxRelayService,
) {
    @PostMapping("/process")
    fun processPendingMessages(): ResponseEntity<Map<String, Any>> {
        // Implementation details...
    }

    @PostMapping("/replay/{messageId}")
    fun replayMessage(
        @PathVariable messageId: String,
    ): ResponseEntity<Map<String, Any>> {
        // Implementation details...
    }

    @PostMapping("/replay-range")
    fun replayMessagesByTimeRange(
        @RequestParam startTime: Instant,
        @RequestParam endTime: Instant,
    ): ResponseEntity<Map<String, Any>> {
        // Implementation details...
    }

    @GetMapping("/pending-count")
    fun getPendingMessageCount(): ResponseEntity<Map<String, Any>> {
        // Implementation details...
    }
}
```

## How It Works

1. **Message Creation**: Services create `OutboxMessage` entities within the same database transaction as their domain data changes. Initially, messages have a status of `PENDING`.

2. **Scheduled Processing**: The `ScheduledOutboxProcessor` runs every 5 seconds to check for pending messages and processes them using the `OutboxRelayService`.

3. **Message Processing**: The `OutboxRelayService` processes messages in batches, sending them to Kafka within a transaction. Successfully processed messages are marked as `SENT`, while failed messages are marked as `FAILED`.

4. **Manual Operations**: The `OutboxController` provides REST endpoints for manual processing, replaying specific messages, or replaying messages within a time range.

5. **Monitoring**: Metrics are collected for monitoring the number of pending messages, processed messages, failed messages, and processing latency.

## Configuration

To enable the polling relay, set the following property in your application configuration:

```properties
outbox.relay.enabled=true
```

By default, the relay is enabled unless explicitly disabled.

## Usage

### Creating Outbox Messages

Services should create outbox messages within the same transaction as their domain data changes:

```kotlin
@Transactional
fun handle(command: SomeCommand): UUID {
    // Save domain entity
    val entity = SomeEntity(/* ... */)
    val saved = repository.save(entity)
    
    // Create outbox message
    val outbox = OutboxMessage(
        aggregateId = saved.id!!.toString(),
        aggregateType = "SomeEntity",
        eventType = "SomeEntityCreated",
        payload = serializePayload(saved),
        status = OutboxStatus.PENDING,
        occurredAt = Instant.now(),
    )
    outboxRepository.save(outbox)
    
    return saved.id!!
}
```

### Processing Messages

Messages are automatically processed by the scheduled processor. You can also manually trigger processing:

```bash
curl -X POST http://localhost:8080/api/outbox/process
```

### Replaying Messages

To replay a specific message:

```bash
curl -X POST http://localhost:8080/api/outbox/replay/{messageId}
```

To replay messages within a time range:

```bash
curl -X POST "http://localhost:8080/api/outbox/replay-range?startTime=2023-01-01T00:00:00Z&endTime=2023-01-02T00:00:00Z"
```

### Checking Pending Count

To check the number of pending messages:

```bash
curl -X GET http://localhost:8080/api/outbox/pending-count
```

## Benefits

1. **Fallback Mechanism**: Provides a reliable fallback when Debezium CDC is not available
2. **Operational Flexibility**: Allows manual intervention when needed
3. **Monitoring**: Built-in metrics and health indicators
4. **Replay Capability**: Can replay specific messages for recovery scenarios
5. **Low Overhead**: For low-volume services, scheduled processing has minimal impact

## Trade-offs

1. **Latency**: Slightly higher latency compared to Debezium's near real-time processing
2. **Resource Usage**: Scheduled processing consumes resources even during low activity periods
3. **Complexity**: Maintaining two mechanisms increases overall system complexity

## Best Practices

1. **Use Debezium When Possible**: Prefer Debezium CDC over the polling relay when available
2. **Monitor Pending Messages**: Keep track of the number of pending messages to ensure healthy operation
3. **Handle Failures Gracefully**: Implement proper error handling and retry mechanisms
4. **Configure Batch Size**: Adjust the batch size based on your throughput requirements
5. **Enable Metrics**: Use the built-in metrics to monitor the relay's performance