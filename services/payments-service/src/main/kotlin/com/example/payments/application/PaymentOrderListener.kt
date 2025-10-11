package com.example.payments.application

import com.example.observability.StructuredLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Component
class PaymentOrderListener(
    private val paymentService: PaymentService,
) {
    private val logger = StructuredLogger.getLogger(PaymentOrderListener::class.java)
    private val json = Json { ignoreUnknownKeys = false }

    @KafkaListener(topics = [ORDERS_CREATED_TOPIC], containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    @Suppress("TooGenericExceptionCaught")
    fun onOrderCreated(record: ConsumerRecord<String, String>) {
        runCatching {
            logger.info(
                "Received order created event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "value" to record.value(),
            )

            val event = OrderCreatedEventCodec.decode(record.value())
            val eventId = UUID.fromString(event.eventId.toString())

            val payload = json.parseToJsonElement(event.payload).jsonObject
            val itemCount = payload["itemCount"]!!.jsonPrimitive.int
            val amount = amountFromItemCount(itemCount)

            logger.info(
                "Processing payment for order",
                "orderId" to event.aggregateId.toString(),
                "itemCount" to itemCount,
                "amount" to amount,
                "eventId" to eventId,
            )

            paymentService.handle(
                ProcessPaymentCommand(
                    orderId = UUID.fromString(event.aggregateId.toString()),
                    amount = amount,
                    eventId = eventId,
                    metadata =
                        mapOf(
                            "itemCount" to itemCount.toString(),
                        ),
                ),
            )

            logger.info(
                "Payment processed successfully",
                "orderId" to event.aggregateId.toString(),
                "eventId" to eventId,
            )
        }.onFailure { throwable ->
            logger.error(
                "Failed to process order created event",
                "topic" to record.topic(),
                "partition" to record.partition(),
                "offset" to record.offset(),
                "key" to record.key(),
                "error" to throwable.message,
            )
            throw throwable
        }
    }

    private fun amountFromItemCount(itemCount: Int): BigDecimal =
        BigDecimal.valueOf(itemCount.toLong()).multiply(BigDecimal.TEN)

    companion object {
        const val ORDERS_CREATED_TOPIC = "orders.created"
    }
}
