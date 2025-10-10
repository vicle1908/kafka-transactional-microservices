package com.example.notification.application

import com.example.notification.domain.NotificationEntity
import com.example.notification.domain.NotificationRepository
import com.example.notification.domain.NotificationStatus
import com.example.outbox.entity.OutboxMessage
import com.example.outbox.repository.OutboxRepository
import com.example.saga.SagaMetricsRecorder
import com.example.saga.SagaNames
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.retry.support.RetryTemplate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class NotificationServiceTest {
    @MockK(relaxed = true)
    private lateinit var notificationRepository: NotificationRepository

    @MockK
    private lateinit var outboxRepository: OutboxRepository

    @MockK
    private lateinit var notificationSenderRegistry: NotificationSenderRegistry

    @MockK
    private lateinit var sagaStateService: SagaStateService

    @MockK
    private lateinit var sagaMetricsRecorder: SagaMetricsRecorder

    private lateinit var retryTemplate: RetryTemplate
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        retryTemplate =
            RetryTemplate
                .builder()
                .maxAttempts(1)
                .fixedBackoff(1)
                .retryOn(NotificationDispatchException::class.java)
                .build()

        notificationService =
            NotificationService(
                notificationRepository = notificationRepository,
                outboxRepository = outboxRepository,
                notificationSenderRegistry = notificationSenderRegistry,
                notificationRetryTemplate = retryTemplate,
                sagaStateService = sagaStateService,
                sagaMetrics = sagaMetricsRecorder,
            )
    }

    @Test
    fun `send should persist notification enqueue outbox entry and mark saga step`() {
        val orderId = UUID.randomUUID()
        val savedNotificationId = UUID.randomUUID()
        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = "email",
                template = "order-confirmation",
                payload = """{"message":"thanks"}""",
            )

        val savedStatuses = mutableListOf<NotificationStatus>()
        every { notificationRepository.save(any()) } answers {
            val entity = firstArg<NotificationEntity>()
            if (entity.id == null) {
                entity.assignId(savedNotificationId)
            }
            savedStatuses += entity.status()
            entity
        }
        every { notificationSenderRegistry.dispatch(command) } just Runs
        val outboxSlot = slot<OutboxMessage>()
        every { outboxRepository.save(capture(outboxSlot)) } answers { firstArg() }
        val successOptions = slot<SagaTransitionOptions>()
        every {
            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = orderId.toString(),
                newState = SagaStatus.COMPLETED,
                options = capture(successOptions),
            )
        } returns mockk(relaxed = true)
        every {
            sagaMetricsRecorder.recordStep(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                step = SagaStepNames.NOTIFICATION_SENT,
                state = SagaStatus.COMPLETED,
            )
        } returns Unit

        val result = notificationService.send(command)

        assertThat(result).isEqualTo(savedNotificationId)
        assertThat(savedStatuses).containsExactly(NotificationStatus.QUEUED, NotificationStatus.SENT)
        assertThat(outboxSlot.captured.aggregateId).isEqualTo(savedNotificationId.toString())
        assertThat(outboxSlot.captured.eventType).isEqualTo("NotificationSent")

        verify(exactly = 1) {
            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = orderId.toString(),
                newState = SagaStatus.COMPLETED,
                options = successOptions.captured,
            )
        }
        verify {
            sagaMetricsRecorder.recordStep(
                SagaNames.ORDER_FULFILLMENT,
                SagaStepNames.NOTIFICATION_SENT,
                SagaStatus.COMPLETED,
            )
        }
    }

    @Test
    fun `send should mark notification failed and propagate exception when dispatch fails`() {
        val orderId = UUID.randomUUID()
        val initialNotificationId = UUID.randomUUID()
        val command =
            SendNotificationCommand(
                orderId = orderId,
                channel = "email",
                template = "order-confirmation",
                payload = """{"message":"thanks"}""",
            )

        val savedStatuses = mutableListOf<NotificationStatus>()
        val failureReasons = mutableListOf<String?>()
        every { notificationRepository.save(any()) } answers {
            val entity = firstArg<NotificationEntity>()
            if (entity.id == null) {
                entity.assignId(initialNotificationId)
            }
            savedStatuses += entity.status()
            failureReasons += entity.failureReason()
            entity
        }
        every { notificationSenderRegistry.dispatch(command) } throws NotificationDispatchException("SMTP unavailable")
        val failureOptions = slot<SagaTransitionOptions>()
        every {
            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = orderId.toString(),
                newState = SagaStatus.FAILED,
                options = capture(failureOptions),
            )
        } returns mockk(relaxed = true)
        every {
            sagaMetricsRecorder.recordStep(
                SagaNames.ORDER_FULFILLMENT,
                SagaStepNames.NOTIFICATION_FAILED,
                SagaStatus.FAILED,
            )
        } returns Unit

        val exception = assertThrows<NotificationDispatchException> { notificationService.send(command) }
        assertThat(exception).hasMessage("SMTP unavailable")

        assertThat(savedStatuses).containsExactly(NotificationStatus.QUEUED, NotificationStatus.FAILED)
        assertThat(failureReasons.last()).isEqualTo("SMTP unavailable")

        verify(exactly = 0) { outboxRepository.save(any()) }
        verify {
            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = orderId.toString(),
                newState = SagaStatus.FAILED,
                options = failureOptions.captured,
            )
        }
        verify {
            sagaMetricsRecorder.recordStep(
                SagaNames.ORDER_FULFILLMENT,
                SagaStepNames.NOTIFICATION_FAILED,
                SagaStatus.FAILED,
            )
        }
    }

    private fun NotificationEntity.assignId(id: UUID): NotificationEntity {
        val field = NotificationEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
        return this
    }
}
