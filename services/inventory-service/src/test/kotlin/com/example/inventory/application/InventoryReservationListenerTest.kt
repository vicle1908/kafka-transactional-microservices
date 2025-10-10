package com.example.inventory.application

import com.example.events.avro.PaymentCompletedEvent
import com.example.inventory.InventoryServiceApplication
import com.example.inventory.domain.InventoryReservationRepository
import com.example.inventory.domain.InventoryStockEntity
import com.example.inventory.domain.InventoryStockRepository
import com.example.inventory.domain.ProcessedEventRepository
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
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest(classes = [InventoryServiceApplication::class])
@EmbeddedKafka(
    partitions = 1,
    controlledShutdown = true,
    topics = [InventoryReservationListener.PAYMENTS_COMPLETED_TOPIC],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
@ActiveProfiles("test")
class InventoryReservationListenerTest {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var reservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var processedEventRepository: ProcessedEventRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    @Autowired
    private lateinit var stockRepository: InventoryStockRepository

    private val json = Json { ignoreUnknownKeys = false }


    @BeforeEach
    fun cleanRepositories() {
        reservationRepository.deleteAll()
        processedEventRepository.deleteAll()
        sagaStateRepository.deleteAll()
        stockRepository.deleteAll()
    }

    @Test
    fun `listener should reserve inventory once per payment event`() {
        val eventId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
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

        val payloadJson =
            buildJsonObject {
                put("orderId", orderId.toString())
                put("amount", "49.95")
                put("status", "COMPLETED")
                put("preferredSku", "sku-123")
                put("quantity", 3)
                putJsonArray("notes") { }
            }
        val payload = json.encodeToString(JsonObject.serializer(), payloadJson)

        stockRepository.save(
            InventoryStockEntity(
                sku = "sku-123",
                availableQuantity = 20,
            ),
        )

        val event =
            PaymentCompletedEvent
                .newBuilder()
                .setEventId(eventId)
                .setAggregateId(paymentId)
                .setOccurredAt(Instant.now())
                .setPayload(payload)
                .build()

        val encoded = PaymentCompletedEventCodec.encode(event)
        kafkaTemplate.executeInTransaction { template ->
            template.send(
                ProducerRecord(
                    InventoryReservationListener.PAYMENTS_COMPLETED_TOPIC,
                    paymentId.toString(),
                    encoded,
                ),
            )
            Unit
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(reservationRepository.count()).isEqualTo(1)
            assertThat(processedEventRepository.existsById(eventId)).isTrue()
        }

        val reservation = reservationRepository.findAll().first()
        assertThat(reservation.orderId).isEqualTo(orderId)
        assertThat(reservation.quantity).isEqualTo(3)
        assertThat(reservation.sku).isEqualTo("sku-123")

        // duplicate event should be ignored
        kafkaTemplate.executeInTransaction { template ->
            template.send(
                ProducerRecord(
                    InventoryReservationListener.PAYMENTS_COMPLETED_TOPIC,
                    paymentId.toString(),
                    encoded,
                ),
            )
            Unit
        }

        Thread.sleep(500)

        assertThat(reservationRepository.count()).isEqualTo(1)
        assertThat(processedEventRepository.count()).isEqualTo(1)

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)
        assertThat(saga.data()).contains(SagaStepNames.INVENTORY_RESERVED)
    }
}
