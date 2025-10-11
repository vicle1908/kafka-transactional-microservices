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
    @Suppress("LongMethod", "TooGenericExceptionCaught")
    fun onInventoryReserved(record: ConsumerRecord<String, String>) {
        try {
            logger.info(
                "Received inventory reserved event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "value" to record.value(),
            )
            val event = InventoryReservedEventCodec.decode(record.value())
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
            val channel = payload["channel"]?.jsonPrimitive?.content ?: DEFAULT_CHANNEL
            val template = payload["template"]?.jsonPrimitive?.content ?: DEFAULT_TEMPLATE
            val body =
                payload["payload"]?.jsonPrimitive?.content
                    ?: json.encodeToString(JsonObject.serializer(), payload)

            logger.info(
                "Sending notification for reserved inventory",
                "orderId" to orderId,
                "channel" to channel,
                "template" to template,
            )

            notificationService.send(
                SendNotificationCommand(
                    orderId = orderId,
                    channel = channel,
                    template = template,
                    payload = body,
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
                "orderId" to orderId,
                "eventId" to eventId,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process inventory reserved event",
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
        const val INVENTORY_RESERVED_TOPIC = "inventory.reserved"
        private const val DEFAULT_CHANNEL = "email"
        private const val DEFAULT_TEMPLATE = "order-confirmation"
    }
}
