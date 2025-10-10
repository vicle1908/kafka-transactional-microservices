package com.example.payments.application

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
    private val json = Json { ignoreUnknownKeys = false }

    @KafkaListener(topics = [ORDERS_CREATED_TOPIC], containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    fun onOrderCreated(record: ConsumerRecord<String, String>) {
        val event = OrderCreatedEventCodec.decode(record.value())
        val eventId = UUID.fromString(event.eventId.toString())

        val payload = json.parseToJsonElement(event.payload).jsonObject
        val itemCount = payload["itemCount"]!!.jsonPrimitive.int
        val amount = amountFromItemCount(itemCount)

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
    }

    private fun amountFromItemCount(itemCount: Int): BigDecimal =
        BigDecimal.valueOf(itemCount.toLong()).multiply(BigDecimal.TEN)

    companion object {
        const val ORDERS_CREATED_TOPIC = "orders.created"
    }
}
