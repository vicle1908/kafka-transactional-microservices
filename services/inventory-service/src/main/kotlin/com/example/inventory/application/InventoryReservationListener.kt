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
    fun onPaymentCompleted(record: ConsumerRecord<String, String>) {
        runCatching {
            val event = PaymentCompletedEventCodec.decode(record.value())
            val eventId = UUID.fromString(event.eventId.toString())

            if (processedEventRepository.existsById(eventId)) {
                logger.debug(
                    "Event already processed, skipping",
                    "eventId" to eventId,
                )
                return@runCatching
            }

            val reservation = reservationDetails(event.payload)

            logger.info(
                "Reserving inventory for payment",
                "orderId" to reservation.orderId,
                "sku" to reservation.sku,
                "quantity" to reservation.quantity,
            )

            inventoryService.reserve(
                ReserveInventoryCommand(
                    orderId = reservation.orderId,
                    sku = reservation.sku,
                    quantity = reservation.quantity,
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
                "orderId" to reservation.orderId,
                "eventId" to eventId,
            )
        }.onFailure { throwable ->
            logger.error(
                "Failed to process payment completed event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "error" to throwable.message,
            )
            throw throwable
        }
    }

    companion object {
        const val PAYMENTS_COMPLETED_TOPIC = "payments.completed"
        private const val DEFAULT_SKU = "generic-sku"
        private const val DEFAULT_QUANTITY = 1
    }

    private fun reservationDetails(payload: String): ReservationDetails {
        val jsonPayload = json.parseToJsonElement(payload).jsonObject
        val orderId = UUID.fromString(jsonPayload["orderId"]!!.jsonPrimitive.content)
        val sku =
            jsonPayload["preferredSku"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: DEFAULT_SKU
        val quantity = jsonPayload["quantity"]?.jsonPrimitive?.int ?: DEFAULT_QUANTITY
        return ReservationDetails(orderId, sku, quantity)
    }

    private data class ReservationDetails(
        val orderId: UUID,
        val sku: String,
        val quantity: Int,
    )
}
