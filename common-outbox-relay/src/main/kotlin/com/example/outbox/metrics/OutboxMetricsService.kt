package com.example.outbox.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class OutboxMetricsService(
    private val meterRegistry: MeterRegistry,
) {
    private val pendingMessagesGauge = AtomicLong(0)
    private val processedMessagesCounter: Counter
    private val failedMessagesCounter: Counter
    private val relayLatencyTimer: Timer
    private val endToEndLatencyTimer: Timer
    private val debeziumLagGauge = AtomicLong(0)

    init {
        // Register gauges
        meterRegistry.gauge("outbox.pending.messages", pendingMessagesGauge)
        meterRegistry.gauge("debezium.offset.lag.records", debeziumLagGauge)

        // Register counters
        processedMessagesCounter =
            Counter
                .builder("outbox.processed.messages")
                .description("Number of outbox messages successfully processed")
                .register(meterRegistry)

        failedMessagesCounter =
            Counter
                .builder("outbox.failed.messages")
                .description("Number of outbox messages that failed processing")
                .register(meterRegistry)

        // Register timers
        relayLatencyTimer =
            Timer
                .builder("relay.kafka.commit.latency")
                .description("Time taken to commit Kafka transactions in the outbox relay")
                .register(meterRegistry)

        endToEndLatencyTimer =
            Timer
                .builder("outbox.end.to.end.latency")
                .description("End-to-end processing time from message creation to successful publication")
                .register(meterRegistry)
    }

    fun updatePendingMessages(count: Long) {
        pendingMessagesGauge.set(count)
    }

    fun recordProcessedMessage() {
        processedMessagesCounter.increment()
    }

    fun recordFailedMessage() {
        failedMessagesCounter.increment()
    }

    fun recordRelayLatency(latencyMillis: Long) {
        relayLatencyTimer.record(latencyMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    fun recordEndToEndLatency(latencyMillis: Long) {
        endToEndLatencyTimer.record(latencyMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    fun updateDebeziumLag(records: Long) {
        debeziumLagGauge.set(records)
    }

    fun recordConnectorError(connectorName: String) {
        Counter
            .builder("debezium.connector.errors")
            .tag("connector", connectorName)
            .description("Number of errors encountered by Debezium connectors")
            .register(meterRegistry)
            .increment()
    }

    fun updateConnectorStatus(
        connectorName: String,
        isRunning: Boolean,
    ) {
        val value = if (isRunning) 1.0 else 0.0
        meterRegistry.gauge(
            "debezium.connector.status.running",
            listOf(
                io.micrometer.core.instrument.Tag
                    .of("connector", connectorName),
            ),
        ) { value }
    }
}
