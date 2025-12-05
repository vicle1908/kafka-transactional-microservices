package com.example.inventory.adapter.inbound.kafka

import com.example.inventory.application.InventoryService
import com.example.inventory.application.OrderCreatedEventCodec
import com.example.inventory.application.ReserveInventoryCommand
import com.example.inventory.domain.ProcessedEventEntity
import com.example.inventory.domain.ProcessedEventRepository
import com.example.observability.StructuredLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class OrderCreatedListener(
    private val inventoryService: InventoryService,
    private val processedEventRepository: ProcessedEventRepository,
) {
    private val logger = StructuredLogger.getLogger(OrderCreatedListener::class.java)
    private val json = Json.Default

    @KafkaListener(topics = [ORDERS_CREATED_TOPIC])
    @Transactional
    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun handleOrderCreated(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        runCatching {
            logger.info(
                "Received OrderCreatedEvent",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
            )

            val event = OrderCreatedEventCodec.decode(record.value())
            val eventId = UUID.fromString(event.eventId.toString())
            val orderId = event.aggregateId

            if (processedEventRepository.existsById(eventId)) {
                logger.debug(
                    "OrderCreatedEvent already processed, skipping",
                    "eventId" to eventId,
                    "orderId" to orderId,
                )
                acknowledgment.acknowledge()
                return@runCatching
            }

            val payload = parsePayload(event.payload)
            logger.info(
                "Parsed OrderCreatedEvent payload",
                "orderId" to orderId,
                "itemCount" to payload.items.size,
            )

            for (item in payload.items) {
                val sku = item.productId
                val quantity = item.quantity

                logger.info(
                    "Reserving inventory for order item",
                    "orderId" to orderId,
                    "sku" to sku,
                    "quantity" to quantity,
                )

                val reservationId =
                    inventoryService.reserve(
                        ReserveInventoryCommand(
                            orderId = orderId,
                            sku = sku,
                            quantity = quantity,
                        ),
                    )

                logger.info(
                    "Inventory reserved successfully",
                    "orderId" to orderId,
                    "sku" to sku,
                    "quantity" to quantity,
                    "reservationId" to reservationId,
                )
            }

            processedEventRepository.save(
                ProcessedEventEntity(
                    eventId = eventId,
                    processedAt = Instant.now(),
                ),
            )

            acknowledgment.acknowledge()

            logger.info(
                "OrderCreatedEvent processed successfully",
                "eventId" to eventId,
                "orderId" to orderId,
            )
        }.onFailure { throwable ->
            logger.error(
                "Error processing OrderCreatedEvent",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "error" to (throwable.message ?: "Unknown error"),
            )
            throw throwable
        }
    }

    private fun parsePayload(payloadJson: String): OrderCreatedPayload =
        try {
            json.decodeFromString<OrderCreatedPayload>(payloadJson)
        } catch (e: Exception) {
            logger.error("Failed to parse OrderCreatedEvent payload", "payload" to payloadJson, "error" to e.message)
            throw IllegalArgumentException("Invalid payload format", e)
        }

    @Serializable
    private data class OrderCreatedPayload(
        val orderId: String,
        val customerId: String,
        val items: List<OrderItemPayload>,
        val itemCount: Int,
        val totalAmount: String,
    )

    @Serializable
    private data class OrderItemPayload(
        val productId: String,
        val quantity: Int,
        val unitPrice: String,
        val productName: String? = null,
    )

    companion object {
        const val ORDERS_CREATED_TOPIC = "outbox.Order"
    }
}
