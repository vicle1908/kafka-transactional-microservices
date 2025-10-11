package com.example.inventory.application

import com.example.events.avro.InventoryReservedEvent
import com.example.inventory.domain.InventoryReservationEntity
import com.example.inventory.domain.InventoryReservationRepository
import com.example.inventory.domain.InventoryReservationStatus
import com.example.inventory.domain.InventoryStockRepository
import com.example.observability.StructuredLogger
import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import com.example.outbox.repository.OutboxRepository
import com.example.saga.SagaMetricsRecorder
import com.example.saga.SagaNames
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class InventoryService(
    private val reservationRepository: InventoryReservationRepository,
    private val stockRepository: InventoryStockRepository,
    private val outboxRepository: OutboxRepository,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = StructuredLogger.getLogger(InventoryService::class.java)

    @Transactional
    @Suppress("LongMethod")
    fun reserve(command: ReserveInventoryCommand): UUID {
        logger.info(
            "Reserving inventory",
            "orderId" to command.orderId,
            "sku" to command.sku,
            "quantity" to command.quantity,
        )
        validate(command)
        val occurredAt = Instant.now()

        val stock =
            stockRepository.lockBySku(command.sku)
                ?: throw IllegalStateException("Stock not configured for sku=${command.sku}")
        logger.info(
            "Reserving stock",
            "sku" to command.sku,
            "quantity" to command.quantity,
        )
        stock.reserve(command.quantity)

        val reservation =
            InventoryReservationEntity(
                orderId = command.orderId,
                sku = command.sku,
                quantity = command.quantity,
                status = InventoryReservationStatus.RESERVED,
                reservedAt = occurredAt,
                updatedAt = occurredAt,
            )
        val saved = reservationRepository.save(reservation)
        stockRepository.save(stock)

        logger.info(
            "Inventory reservation saved",
            "reservationId" to saved.id,
            "orderId" to command.orderId,
        )

        val event =
            InventoryReservedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(saved.id!!)
                .setOccurredAt(occurredAt)
                .setPayload(serializePayload(saved))
                .build()

        val payload = encodeEvent(event)

        val outbox =
            OutboxMessage(
                aggregateId = saved.id!!.toString(),
                aggregateType = "InventoryReservation",
                eventType = "InventoryReserved",
                payload = payload,
                headers = null,
                status = OutboxStatus.PENDING,
                occurredAt = occurredAt,
            )
        outboxRepository.save(outbox)

        logger.info(
            "Outbox message created for inventory reservation",
            "reservationId" to saved.id,
            "orderId" to command.orderId,
        )

        // Publish stock change event for after-commit eviction
        eventPublisher.publishEvent(InventoryStockChangedEvent(command.sku))
        logger.debug("Stock change event published", "sku" to command.sku)

        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = command.orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.IN_PROGRESS,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.INVENTORY_RESERVED, occurredAt)
                    },
                    occurredAt = occurredAt,
                ),
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.INVENTORY_RESERVED,
            state = SagaStatus.IN_PROGRESS,
        )

        logger.info(
            "Inventory reservation completed",
            "reservationId" to saved.id,
            "orderId" to command.orderId,
        )

        return saved.id!!
    }

    @Transactional
    @Suppress("LongMethod")
    fun release(
        orderId: UUID,
        reason: String?,
    ) {
        logger.info(
            "Releasing inventory reservations",
            "orderId" to orderId,
            "reason" to reason,
        )
        val occurredAt = Instant.now()

        val reservations = reservationRepository.findAllByOrderId(orderId)
        reservations.forEach { reservation ->
            val stock =
                stockRepository.lockBySku(reservation.sku)
                    ?: throw IllegalStateException("Stock not configured for sku=${reservation.sku}")
            logger.info(
                "Releasing stock",
                "sku" to reservation.sku,
                "quantity" to reservation.quantity,
            )
            stock.release(reservation.quantity)
            stockRepository.save(stock)
        }

        logger.info(
            "Inventory released",
            "orderId" to orderId,
            "reservationCount" to reservations.size,
        )

        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.COMPENSATING,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.IN_PROGRESS,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.INVENTORY_RELEASED, occurredAt)
                    },
                    occurredAt = occurredAt,
                ),
        )
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.FAILED,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.COMPENSATING,
                    dataTransformer = { existing ->
                        val failureLabel = buildReleaseLabel(reason)
                        SagaStepFormatter.append(existing, failureLabel, occurredAt)
                    },
                    occurredAt = occurredAt,
                ),
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.INVENTORY_RELEASED,
            state = SagaStatus.FAILED,
        )

        logger.info(
            "Inventory release saga updated",
            "orderId" to orderId,
        )

        // Publish stock change events for all impacted SKUs
        reservations.map { it.sku }.toSet().forEach { sku ->
            eventPublisher.publishEvent(InventoryStockChangedEvent(sku))
            logger.debug("Stock change event published", "sku" to sku)
        }
    }

    private fun validate(command: ReserveInventoryCommand) {
        require(command.sku.isNotBlank()) { "sku must not be blank" }
        require(command.quantity > 0) { "quantity must be positive" }
    }

    private fun serializePayload(reservation: InventoryReservationEntity): String {
        val payload =
            InventoryReservedPayload(
                reservationId = reservation.id!!.toString(),
                orderId = reservation.orderId.toString(),
                sku = reservation.sku,
                quantity = reservation.quantity,
                status = reservation.status.name,
            )
        return json.encodeToString(payload)
    }

    private fun encodeEvent(event: InventoryReservedEvent): String {
        val writer = SpecificDatumWriter(InventoryReservedEvent::class.java)
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(InventoryReservedEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }

    @Serializable
    private data class InventoryReservedPayload(
        val reservationId: String,
        val orderId: String,
        val sku: String,
        val quantity: Int,
        val status: String,
    )

    private companion object {
        val json = Json.Default

        private fun buildReleaseLabel(reason: String?): String =
            if (reason.isNullOrBlank()) {
                SagaStepNames.INVENTORY_RELEASED
            } else {
                "${SagaStepNames.INVENTORY_RELEASED}:$reason"
            }
    }
}
