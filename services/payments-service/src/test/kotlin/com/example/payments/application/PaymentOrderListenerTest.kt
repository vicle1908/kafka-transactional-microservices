package com.example.payments.application

import com.example.events.avro.OrderCreatedEvent
import com.example.outbox.repository.OutboxRepository
import com.example.payments.PaymentServiceIntegrationTestSupport
import com.example.payments.PaymentsServiceApplication
import com.example.payments.domain.PaymentRepository
import com.example.payments.domain.ProcessedEventRepository
import com.example.saga.SagaNames
import com.example.saga.SagaStateRepository
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest(classes = [PaymentsServiceApplication::class])
@EmbeddedKafka(partitions = 1, controlledShutdown = true, topics = [PaymentOrderListener.ORDERS_CREATED_TOPIC])
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentOrderListenerTest : PaymentServiceIntegrationTestSupport() {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var processedEventRepository: ProcessedEventRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    private val json = Json { ignoreUnknownKeys = false }

    @BeforeEach
    fun cleanRepositories() {
        paymentRepository.deleteAll()
        processedEventRepository.deleteAll()
        sagaStateRepository.deleteAll()
        outboxRepository.deleteAll()
    }

    @Test
    fun `listener processes order created event exactly once`() {
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val payloadJson =
            buildJsonObject {
                put("orderId", orderId.toString())
                put("customerId", "customer-123")
                putJsonArray("items") {
                    add("item-1")
                    add("item-2")
                }
                put("itemCount", 2)
            }
        val payload = json.encodeToString(JsonObject.serializer(), payloadJson)

        val createdAt = Instant.now()
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, createdAt),
            at = createdAt,
        )

        val event =
            OrderCreatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setAggregateId(orderId)
                .setOccurredAt(createdAt)
                .setPayload(payload)
                .build()

        val encoded = OrderCreatedEventCodec.encode(event)
        kafkaTemplate.executeInTransaction { template ->
            template.send(ProducerRecord(PaymentOrderListener.ORDERS_CREATED_TOPIC, orderId.toString(), encoded))
            Unit
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(paymentRepository.count()).isEqualTo(1)
            assertThat(processedEventRepository.existsById(eventId)).isTrue()
        }

        // Send duplicate to prove idempotency
        kafkaTemplate.executeInTransaction { template ->
            template.send(ProducerRecord(PaymentOrderListener.ORDERS_CREATED_TOPIC, orderId.toString(), encoded))
            Unit
        }

        Thread.sleep(500) // allow listener to process duplicate if it were to

        assertThat(paymentRepository.count()).isEqualTo(1)
        assertThat(processedEventRepository.count()).isEqualTo(1)

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)
        assertThat(saga.data()).contains(SagaStepNames.PAYMENT_COMPLETED)
    }

    @Test
    fun `listener records failed payment when gateway declines`() {
        val eventId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val payloadJson =
            buildJsonObject {
                put("orderId", orderId.toString())
                put("customerId", "customer-456")
                putJsonArray("items") {
                    repeat(5) { add("expensive-item-$it") }
                }
                put("itemCount", 200)
            }
        val payload = json.encodeToString(JsonObject.serializer(), payloadJson)

        val createdAt = Instant.now()
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, createdAt),
            at = createdAt,
        )

        val event =
            OrderCreatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setAggregateId(orderId)
                .setOccurredAt(createdAt)
                .setPayload(payload)
                .build()
        val encoded = OrderCreatedEventCodec.encode(event)

        kafkaTemplate.executeInTransaction { template ->
            template.send(ProducerRecord(PaymentOrderListener.ORDERS_CREATED_TOPIC, orderId.toString(), encoded))
            Unit
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(paymentRepository.count()).isEqualTo(1)
            assertThat(outboxRepository.count()).isEqualTo(1)
            assertThat(processedEventRepository.existsById(eventId)).isTrue()
        }

        val payment = paymentRepository.findAll().first()
        assertThat(payment.status()).isEqualTo(com.example.payments.domain.PaymentStatus.FAILED)
        assertThat(payment.failureReason()).isNotBlank

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.data()).contains(SagaStepNames.PAYMENT_FAILED)
    }
}
