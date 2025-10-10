package com.example.notification.application

import com.example.events.avro.InventoryReservedEvent
import com.example.notification.NotificationServiceApplication
import com.example.notification.domain.NotificationRepository
import com.example.notification.domain.NotificationStatus
import com.example.notification.domain.ProcessedEventRepository
import com.example.saga.SagaNames
import com.example.saga.SagaStateRepository
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest(classes = [NotificationServiceApplication::class])
@EmbeddedKafka(
    partitions = 1,
    controlledShutdown = true,
    topics = [NotificationListener.INVENTORY_RESERVED_TOPIC],
)
@ActiveProfiles("test")
class NotificationListenerTest {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var processedEventRepository: ProcessedEventRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    private val json = Json { ignoreUnknownKeys = false }

    @BeforeEach
    fun cleanRepositories() {
        notificationRepository.deleteAll()
        processedEventRepository.deleteAll()
        sagaStateRepository.deleteAll()
    }

    @Test
    fun `listener should send notification once per reservation`() {
        val eventId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        primeSagaBeforeNotification(orderId)
        val payloadJson =
            buildJsonObject {
                put("reservationId", reservationId.toString())
                put("orderId", orderId.toString())
                put("channel", "email")
                put("template", "order-confirmation")
                put("payload", """{"message":"Your order is ready"}""")
                putJsonArray("metadata") { }
            }
        val payload = json.encodeToString(JsonObject.serializer(), payloadJson)

        val event =
            InventoryReservedEvent
                .newBuilder()
                .setEventId(eventId)
                .setAggregateId(reservationId)
                .setOccurredAt(Instant.now())
                .setPayload(payload)
                .build()

        val encoded = InventoryReservedEventCodec.encode(event)
        kafkaTemplate.executeInTransaction { template ->
            template.send(
                ProducerRecord(
                    NotificationListener.INVENTORY_RESERVED_TOPIC,
                    reservationId.toString(),
                    encoded,
                ),
            )
            Unit
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(notificationRepository.count()).isEqualTo(1)
            assertThat(processedEventRepository.existsById(eventId)).isTrue()
        }

        val notification = notificationRepository.findAll().first()
        assertThat(notification.orderId).isEqualTo(orderId)
        assertThat(notification.channel).isEqualTo("email")
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT)

        kafkaTemplate.executeInTransaction { template ->
            template.send(
                ProducerRecord(
                    NotificationListener.INVENTORY_RESERVED_TOPIC,
                    reservationId.toString(),
                    encoded,
                ),
            )
            Unit
        }

        Thread.sleep(500)

        assertThat(notificationRepository.count()).isEqualTo(1)
        assertThat(processedEventRepository.count()).isEqualTo(1)

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.COMPLETED)
        assertThat(saga.data()).contains(SagaStepNames.NOTIFICATION_SENT)
    }

    private fun primeSagaBeforeNotification(orderId: UUID) {
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.STARTED,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.PAYMENT_COMPLETED, creationInstant)
                    },
                    occurredAt = creationInstant,
                ),
        )
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.IN_PROGRESS,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.INVENTORY_RESERVED, creationInstant)
                    },
                    occurredAt = creationInstant,
                ),
        )
    }
}
