package com.example.outbox.service

import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import com.example.outbox.metrics.OutboxMetricsService
import com.example.outbox.repository.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Service
class OutboxRelayService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val transactionTemplate: TransactionTemplate,
    private val kafkaTransactionManager: KafkaTransactionManager<String, Any>,
    private val metricsService: OutboxMetricsService,
) {
    private val logger = LoggerFactory.getLogger(OutboxRelayService::class.java)
    private val batchSize = 100

    init {
        // Initialize metrics with current pending count
        updatePendingMessageCountMetric()
    }

    /**
     * Process pending outbox messages in batches
     */
    @Transactional
    fun processPendingMessages(): Int {
        logger.info("Starting outbox relay processing")

        var processedCount = 0
        var hasMoreMessages = true

        while (hasMoreMessages) {
            val pendingMessages =
                outboxRepository.findByStatusOrderByCreatedAtAsc(
                    OutboxStatus.PENDING,
                    PageRequest.of(0, batchSize),
                )

            if (pendingMessages.isEmpty()) {
                hasMoreMessages = false
                continue
            }

            logger.info("Processing batch of ${pendingMessages.size} pending outbox messages")

            pendingMessages.forEach { message ->
                try {
                    val startTime = System.currentTimeMillis()
                    processMessage(message)
                    val endTime = System.currentTimeMillis()

                    metricsService.recordProcessedMessage()
                    metricsService.recordEndToEndLatency(endTime - startTime)
                    processedCount++
                } catch (e: Exception) {
                    logger.error("Failed to process outbox message with id: ${message.id}", e)
                    markMessageAsFailed(message)
                    metricsService.recordFailedMessage()
                }
            }

            // If we got less than batch size, there are no more messages
            if (pendingMessages.size < batchSize) {
                hasMoreMessages = false
            }
        }

        // Update pending count metric
        updatePendingMessageCountMetric()

        logger.info("Finished outbox relay processing. Processed $processedCount messages")
        return processedCount
    }

    /**
     * Process messages within a specific time range
     */
    @Transactional
    fun processMessagesByTimeRange(
        startTime: Instant,
        endTime: Instant,
    ): Int {
        logger.info("Processing outbox messages in time range: $startTime to $endTime")

        var processedCount = 0
        var hasMoreMessages = true
        var offset = 0

        while (hasMoreMessages) {
            val messages =
                outboxRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                    startTime,
                    endTime,
                    PageRequest.of(offset / batchSize, batchSize),
                )

            if (messages.isEmpty()) {
                hasMoreMessages = false
                continue
            }

            logger.info("Processing batch of ${messages.size} outbox messages from time range")

            messages.forEach { message ->
                try {
                    val startTimeMs = System.currentTimeMillis()
                    processMessage(message)
                    val endTimeMs = System.currentTimeMillis()

                    metricsService.recordProcessedMessage()
                    metricsService.recordEndToEndLatency(endTimeMs - startTimeMs)
                    processedCount++
                } catch (e: Exception) {
                    logger.error("Failed to process outbox message with id: ${message.id}", e)
                    markMessageAsFailed(message)
                    metricsService.recordFailedMessage()
                }
            }

            // If we got less than batch size, there are no more messages
            if (messages.size < batchSize) {
                hasMoreMessages = false
            }

            offset += batchSize
        }

        // Update pending count metric
        updatePendingMessageCountMetric()

        logger.info("Finished processing messages in time range. Processed $processedCount messages")
        return processedCount
    }

    /**
     * Replay a specific message by ID
     */
    @Transactional
    fun replayMessage(messageId: String): Boolean =
        try {
            val uuid = java.util.UUID.fromString(messageId)
            val message = outboxRepository.findById(uuid)

            if (message.isPresent) {
                logger.info("Replaying outbox message with id: $messageId")
                val startTime = System.currentTimeMillis()
                processMessage(message.get())
                val endTime = System.currentTimeMillis()

                metricsService.recordProcessedMessage()
                metricsService.recordEndToEndLatency(endTime - startTime)
                true
            } else {
                logger.warn("Outbox message with id $messageId not found")
                false
            }
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid UUID format: $messageId", e)
            false
        }

    /**
     * Get the current depth of pending outbox messages
     */
    fun getPendingMessageCount(): Long {
        val count = outboxRepository.countByStatus(OutboxStatus.PENDING)
        metricsService.updatePendingMessages(count)
        return count
    }

    /**
     * Update the pending message count metric
     */
    private fun updatePendingMessageCountMetric() {
        val count = outboxRepository.countByStatus(OutboxStatus.PENDING)
        metricsService.updatePendingMessages(count)
    }

    /**
     * Process a single outbox message
     */
    private fun processMessage(message: OutboxMessage) {
        try {
            // Determine the topic based on the aggregate type or event type
            val topic = determineTopic(message)

            // Extract key from the message if available, otherwise use aggregate ID
            val key = extractKey(message) ?: message.aggregateId

            // Convert payload to the appropriate object or keep as string
            val payload = convertPayload(message)

            // Send message within Kafka transaction
            val sendStartTime = System.currentTimeMillis()
            kafkaTemplate.executeInTransaction { operations ->
                operations.send(topic, key, payload)
                logger.debug("Sent message to topic $topic with key $key")
                true
            }
            val sendEndTime = System.currentTimeMillis()

            metricsService.recordRelayLatency(sendEndTime - sendStartTime)

            // Mark message as sent in the database
            markMessageAsSent(message)
        } catch (e: Exception) {
            logger.error("Failed to process outbox message with id: ${message.id}", e)
            markMessageAsFailed(message)
            metricsService.recordFailedMessage()
            throw e
        }
    }

    /**
     * Determine the Kafka topic based on message metadata
     */
    private fun determineTopic(message: OutboxMessage): String {
        // For now, use a simple mapping based on aggregate type
        // In a real implementation, this might be more sophisticated
        return when (message.aggregateType.lowercase()) {
            "order" -> "orders"
            "payment" -> "payments"
            "inventory" -> "inventory"
            "notification" -> "notifications"
            else -> message.aggregateType.lowercase() + "s"
        }
    }

    /**
     * Extract message key from headers or use default
     */
    private fun extractKey(message: OutboxMessage): String? {
        // In a real implementation, this would parse headers to extract a key
        // For now, we'll return null to use the aggregate ID as key
        return null
    }

    /**
     * Convert payload to the appropriate format
     */
    private fun convertPayload(message: OutboxMessage): Any {
        // For now, we'll send the payload as a string
        // In a real implementation, this might deserialize JSON or convert to Avro
        return message.payload
    }

    /**
     * Mark a message as sent in the database
     */
    private fun markMessageAsSent(message: OutboxMessage) {
        outboxRepository.markAsSent(message.id!!, OutboxStatus.SENT, Instant.now())
        logger.debug("Marked outbox message ${message.id} as SENT")
    }

    /**
     * Mark a message as failed in the database
     */
    private fun markMessageAsFailed(message: OutboxMessage) {
        outboxRepository.markAsFailed(message.id!!, OutboxStatus.FAILED)
        logger.debug("Marked outbox message ${message.id} as FAILED")
    }
}
