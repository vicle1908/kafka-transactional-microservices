package com.example.notification.application

import com.example.notification.domain.ProcessedEventEntity
import com.example.notification.domain.ProcessedEventRepository
import com.example.observability.StructuredLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class NotificationListener(
    private val notificationService: NotificationService,
    private val processedEventRepository: ProcessedEventRepository,
) {
    private val logger = StructuredLogger.getLogger(NotificationListener::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(topics = [INVENTORY_RESERVED_TOPIC], containerFactory = "notificationKafkaListenerContainerFactory")
    @Transactional
    fun onInventoryReserved(record: ConsumerRecord<String, String>) {
        runCatching {
            val event = InventoryReservedEventCodec.decode(record.value())
            val eventId = UUID.fromString(event.eventId.toString())

            if (processedEventRepository.existsById(eventId)) {
                logger.debug(
                    "Event already processed, skipping",
                    "eventId" to eventId,
                )
                return@runCatching
            }

            val details = notificationDetails(event.payload)

            logger.info(
                "Sending notification for reserved inventory",
                "orderId" to details.orderId,
                "channel" to details.channel,
                "template" to details.template,
            )

            notificationService.send(
                SendNotificationCommand(
                    orderId = details.orderId,
                    channel = details.channel,
                    template = details.template,
                    payload = details.body,
                ),
            )

            processedEventRepository.save(
                ProcessedEventEntity(
                    eventId = eventId,
                    processedAt = Instant.now(),
                ),
            )

            logger.info(
                "Notification processed successfully",
                "orderId" to details.orderId,
                "eventId" to eventId,
            )
        }.onFailure { throwable ->
            logger.error(
                "Failed to process inventory reserved event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "error" to throwable.message,
            )
            throw throwable
        }
    }

    private fun notificationDetails(payload: String): NotificationDetails {
        val jsonPayload = json.parseToJsonElement(payload).jsonObject
        val orderId = UUID.fromString(jsonPayload["orderId"]!!.jsonPrimitive.content)
        val channel = jsonPayload["channel"]?.jsonPrimitive?.content ?: DEFAULT_CHANNEL
        val template = jsonPayload["template"]?.jsonPrimitive?.content ?: DEFAULT_TEMPLATE
        val body =
            jsonPayload["payload"]?.jsonPrimitive?.content
                ?: json.encodeToString(JsonObject.serializer(), jsonPayload)

        return NotificationDetails(orderId, channel, template, body)
    }

    private data class NotificationDetails(
        val orderId: UUID,
        val channel: String,
        val template: String,
        val body: String,
    )

    companion object {
        const val INVENTORY_RESERVED_TOPIC = "inventory.reserved"
        private const val DEFAULT_CHANNEL = "email"
        private const val DEFAULT_TEMPLATE = "order-confirmation"
    }
}
