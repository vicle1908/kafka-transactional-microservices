package com.example.outbox.processor

import com.example.outbox.service.OutboxRelayService
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class ScheduledOutboxProcessor(
    private val outboxRelayService: OutboxRelayService,
    meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(ScheduledOutboxProcessor::class.java)
    private val pendingMessagesGauge = AtomicLong(0)

    init {
        Gauge
            .builder("outbox.pending.messages", pendingMessagesGauge) { it.toDouble() }
            .description("Number of pending outbox messages awaiting processing")
            .register(meterRegistry)
    }

    /**
     * Process pending outbox messages every 5 seconds
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    fun processPendingMessages() {
        try {
            logger.debug("Scheduled outbox processing starting...")
            val processedCount = outboxRelayService.processPendingMessages()
            logger.debug("Scheduled outbox processing completed. Processed $processedCount messages")

            // Update the gauge with current pending count
            val pendingCount = outboxRelayService.getPendingMessageCount()
            pendingMessagesGauge.set(pendingCount)
        } catch (e: Exception) {
            logger.error("Error during scheduled outbox processing", e)
        }
    }
}
