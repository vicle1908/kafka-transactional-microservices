package com.example.inventory.application

import com.example.inventory.domain.ProcessedEventEntity
import com.example.inventory.domain.ProcessedEventRepository
import com.example.observability.StructuredLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class InventoryReservationListener(
    private val inventoryService: InventoryService,
    private val processedEventRepository: ProcessedEventRepository,
) {
    private val logger = StructuredLogger.getLogger(InventoryReservationListener::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(topics = [PAYMENTS_COMPLETED_TOPIC], containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    @Suppress("LongMethod", "TooGenericExceptionCaught")
    fun onPaymentCompleted(record: ConsumerRecord<String, String>) {
        try {
            logger.info(
                "Received payment completed event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "value" to record.value(),
            )
            val event = PaymentCompletedEventCodec.decode(record.value())
            val eventId = UUID.fromString(event.eventId.toString())

            if (processedEventRepository.existsById(eventId)) {
                logger.debug(
                    "Event already processed, skipping",
                    "eventId" to eventId,
                )
                return
            }

            val payload = json.parseToJsonElement(event.payload).jsonObject
            val orderId = UUID.fromString(payload["orderId"]!!.jsonPrimitive.content)
            val sku = payload["preferredSku"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: DEFAULT_SKU
            val quantity = payload["quantity"]?.jsonPrimitive?.int ?: DEFAULT_QUANTITY

            logger.info(
                "Reserving inventory for payment",
                "orderId" to orderId,
                "sku" to sku,
                "quantity" to quantity,
            )

            inventoryService.reserve(
                ReserveInventoryCommand(
                    orderId = orderId,
                    sku = sku,
                    quantity = quantity,
                ),
            )

            processedEventRepository.save(
                ProcessedEventEntity(
                    eventId = eventId,
                    processedAt = Instant.now(),
                ),
            )

            logger.info(
                "Inventory reservation processed successfully",
                "orderId" to orderId,
                "eventId" to eventId,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process payment completed event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "error" to e.message,
                "stackTrace" to e.stackTraceToString(),
            )
            throw e
        }
    }

    companion object {
        const val PAYMENTS_COMPLETED_TOPIC = "payments.completed"
        private const val DEFAULT_SKU = "generic-sku"
        private const val DEFAULT_QUANTITY = 1
    }
}
