package com.example.inventory.application

import com.example.inventory.domain.ProcessedEventEntity
import com.example.inventory.domain.ProcessedEventRepository
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
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(topics = [PAYMENTS_COMPLETED_TOPIC], containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    fun onPaymentCompleted(record: ConsumerRecord<String, String>) {
        val event = PaymentCompletedEventCodec.decode(record.value())
        val eventId = UUID.fromString(event.eventId.toString())

        if (processedEventRepository.existsById(eventId)) {
            return
        }

        val payload = json.parseToJsonElement(event.payload).jsonObject
        val orderId = UUID.fromString(payload["orderId"]!!.jsonPrimitive.content)
        val sku = payload["preferredSku"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: DEFAULT_SKU
        val quantity = payload["quantity"]?.jsonPrimitive?.int ?: DEFAULT_QUANTITY

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
    }

    companion object {
        const val PAYMENTS_COMPLETED_TOPIC = "payments.completed"
        private const val DEFAULT_SKU = "generic-sku"
        private const val DEFAULT_QUANTITY = 1
    }
}
