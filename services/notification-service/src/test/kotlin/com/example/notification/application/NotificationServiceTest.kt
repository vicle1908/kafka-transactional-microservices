package com.example.notification.application

import com.example.notification.NotificationServiceApplication
import com.example.notification.config.NotificationChannelProperties
import com.example.notification.domain.NotificationRepository
import com.example.notification.domain.NotificationStatus
import com.example.persistence.outbox.OutboxRepository
import com.example.saga.SagaNames
import com.example.saga.SagaStateRepository
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    classes = [NotificationServiceApplication::class],
    properties = ["spring.kafka.listener.auto-startup=false"],
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationServiceTest {
    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var channelProperties: NotificationChannelProperties

    private val json = Json { ignoreUnknownKeys = false }

    @BeforeEach
    fun cleanRepositories() {
        notificationRepository.deleteAll()
        outboxRepository.deleteAll()
        sagaStateRepository.deleteAll()
    }

    @Test
    fun `send should persist notification and append outbox event`() {
        val counterBefore = metricCount(SagaStepNames.NOTIFICATION_SENT, SagaStatus.COMPLETED)

        val orderId = UUID.randomUUID()
        primeSagaForNotification(orderId)

        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = "email",
                template = "order-confirmation",
                payload = """{"message":"Thank you"}""",
            )

        val notificationId = notificationService.send(command)

        val notifications = notificationRepository.findAll()
        val events = outboxRepository.findAll()

        assertThat(notificationId).isNotNull
        assertThat(notifications).hasSize(1)
        assertThat(events).hasSize(1)

        val storedEvent = events.first()
        val envelope = json.parseToJsonElement(storedEvent.payload).jsonObject
        val payload = json.parseToJsonElement(envelope["payload"]!!.jsonPrimitive.content).jsonObject

        assertThat(envelope["aggregate_id"]!!.jsonPrimitive.content).isEqualTo(notificationId.toString())
        assertThat(payload["notificationId"]!!.jsonPrimitive.content).isEqualTo(notificationId.toString())
        assertThat(payload["orderId"]!!.jsonPrimitive.content).isEqualTo(command.orderId.toString())
        assertThat(payload["channel"]!!.jsonPrimitive.content).isEqualTo(command.channel)
        assertThat(payload["template"]!!.jsonPrimitive.content).isEqualTo(command.template)
        assertThat(payload["payload"]!!.jsonPrimitive.content).isEqualTo(command.payload)
        assertThat(payload["status"]!!.jsonPrimitive.content).isEqualTo("SENT")

        val persisted = notifications.first()
        assertThat(persisted.status()).isEqualTo(NotificationStatus.SENT)
        assertThat(persisted.failureReason()).isNull()

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.COMPLETED)
        assertThat(saga.data()).contains(SagaStepNames.NOTIFICATION_SENT)

        val counterAfter = metricCount(SagaStepNames.NOTIFICATION_SENT, SagaStatus.COMPLETED)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
    }

    @Test
    fun `send should deliver sms notification when provider succeeds`() {
        val orderId = UUID.randomUUID()
        primeSagaForNotification(orderId)
        channelProperties.sms.simulateFailure = false

        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = "sms",
                template = "shipping-update",
                payload = """{"message":"Shipped"}""",
            )

        val notificationId = notificationService.send(command)

        val persisted = notificationRepository.findAll().first()
        assertThat(persisted.status()).isEqualTo(NotificationStatus.SENT)
        assertThat(persisted.id).isEqualTo(notificationId)
        assertThat(outboxRepository.count()).isEqualTo(1)
    }

    @Test
    fun `send should reject invalid input`() {
        val counterBefore = metricCount(SagaStepNames.NOTIFICATION_SENT, SagaStatus.COMPLETED)

        val orderId = UUID.randomUUID()
        primeSagaForNotification(orderId)

        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = " ",
                template = "",
                payload = "",
            )

        assertThatThrownBy { notificationService.send(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("channel")

        assertThat(notificationRepository.count()).isZero()
        assertThat(outboxRepository.count()).isZero()

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)

        val counterAfter = metricCount(SagaStepNames.NOTIFICATION_SENT, SagaStatus.COMPLETED)
        assertThat(counterAfter - counterBefore).isEqualTo(0.0)
    }

    @Test
    fun `send should mark notification failed when dispatch throws`() {
        val counterBefore = metricCount(SagaStepNames.NOTIFICATION_FAILED, SagaStatus.FAILED)

        val orderId = UUID.randomUUID()
        primeSagaForNotification(orderId)
        channelProperties.sms.simulateFailure = true

        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = "sms",
                template = "order-confirmation",
                payload = "{}",
            )

        assertThatThrownBy { notificationService.send(command) }
            .isInstanceOf(NotificationDispatchException::class.java)

        val notifications = notificationRepository.findAll()
        assertThat(notifications).hasSize(1)
        val notification = notifications.first()
        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED)
        assertThat(outboxRepository.count()).isZero()

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.data()).contains(SagaStepNames.NOTIFICATION_FAILED)

        val counterAfter = metricCount(SagaStepNames.NOTIFICATION_FAILED, SagaStatus.FAILED)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)

        channelProperties.sms.simulateFailure = false
    }

    private fun primeSagaForNotification(orderId: UUID) {
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        advanceSaga(orderId, creationInstant, SagaStatus.STARTED, SagaStepNames.PAYMENT_COMPLETED)
        advanceSaga(orderId, creationInstant, SagaStatus.IN_PROGRESS, SagaStepNames.INVENTORY_RESERVED)
    }

    private fun metricCount(
        step: String,
        state: SagaStatus,
    ): Double {
        val counter =
            meterRegistry.counter(
                "saga.step.processed",
                listOf(
                    Tag.of("sagaType", SagaNames.ORDER_FULFILLMENT),
                    Tag.of("step", step),
                    Tag.of("state", state.name),
                ),
            )
        return counter.count()
    }

    private fun advanceSaga(
        orderId: UUID,
        creationInstant: Instant,
        expectedState: SagaStatus,
        nextStep: String,
    ) {
        val options =
            SagaTransitionOptions(
                expectedState = expectedState,
                dataTransformer = { existing ->
                    SagaStepFormatter.append(existing, nextStep, creationInstant)
                },
                occurredAt = creationInstant,
            )
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options = options,
        )
    }
}
