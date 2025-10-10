package com.example.notification.application

import com.example.events.avro.NotificationSentEvent
import com.example.notification.domain.NotificationEntity
import com.example.notification.domain.NotificationRepository
import com.example.notification.domain.NotificationStatus
import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import com.example.outbox.repository.OutboxRepository
import com.example.saga.SagaMetricsRecorder
import com.example.saga.SagaNames
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val outboxRepository: OutboxRepository,
    private val notificationSenderRegistry: NotificationSenderRegistry,
    private val notificationRetryTemplate: RetryTemplate,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
) {
    @Transactional(noRollbackFor = [NotificationDispatchException::class])
    fun send(command: SendNotificationCommand): UUID {
        validate(command)
        val occurredAt = Instant.now()

        val notification =
            NotificationEntity(
                orderId = command.orderId,
                channel = command.channel,
                template = command.template,
                payload = command.payload,
                status = NotificationStatus.QUEUED,
                createdAt = occurredAt,
                updatedAt = occurredAt,
            )
        val saved = notificationRepository.save(notification)

        return try {
            notificationRetryTemplate.execute<Unit, NotificationDispatchException> {
                notificationSenderRegistry.dispatch(command)
            }

            val completedAt = Instant.now()
            notification.markSent(completedAt)
            notificationRepository.save(notification)

            val event =
                NotificationSentEvent
                    .newBuilder()
                    .setEventId(UUID.randomUUID())
                    .setAggregateId(saved.id!!)
                    .setOccurredAt(occurredAt)
                    .setPayload(serializePayload(notification))
                    .build()

            val encoded = encodeEvent(event)

            outboxRepository.save(
                OutboxMessage(
                    aggregateId = saved.id!!.toString(),
                    aggregateType = "Notification",
                    eventType = "NotificationSent",
                    payload = encoded,
                    headers = null,
                    status = OutboxStatus.PENDING,
                    occurredAt = occurredAt,
                ),
            )

            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = command.orderId.toString(),
                newState = SagaStatus.COMPLETED,
                options =
                    SagaTransitionOptions(
                        expectedState = SagaStatus.IN_PROGRESS,
                        dataTransformer = { existing ->
                            SagaStepFormatter.append(existing, SagaStepNames.NOTIFICATION_SENT, completedAt)
                        },
                        occurredAt = completedAt,
                    ),
            )
            sagaMetrics.recordStep(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                step = SagaStepNames.NOTIFICATION_SENT,
                state = SagaStatus.COMPLETED,
            )

            saved.id!!
        } catch (ex: NotificationDispatchException) {
            val failedAt = Instant.now()
            notification.markFailed(failedAt, ex.message)
            notificationRepository.save(notification)
            sagaStateService.transitionByCorrelation(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                correlationId = command.orderId.toString(),
                newState = SagaStatus.FAILED,
                options =
                    SagaTransitionOptions(
                        expectedState = SagaStatus.IN_PROGRESS,
                        dataTransformer = { existing ->
                            val failureLabel = "${SagaStepNames.NOTIFICATION_FAILED}:${ex.message}"
                            SagaStepFormatter.append(existing, failureLabel, failedAt)
                        },
                        occurredAt = failedAt,
                    ),
            )
            sagaMetrics.recordStep(
                sagaType = SagaNames.ORDER_FULFILLMENT,
                step = SagaStepNames.NOTIFICATION_FAILED,
                state = SagaStatus.FAILED,
            )
            throw ex
        }
    }

    private fun validate(command: SendNotificationCommand) {
        require(command.channel.isNotBlank()) { "channel must not be blank" }
        require(command.template.isNotBlank()) { "template must not be blank" }
        require(command.payload.isNotBlank()) { "payload must not be blank" }
    }

    private fun serializePayload(notification: NotificationEntity): String {
        val payload =
            NotificationSentPayload(
                notificationId = notification.id!!.toString(),
                orderId = notification.orderId.toString(),
                channel = notification.channel,
                template = notification.template,
                payload = notification.payload,
                status = notification.status().name,
            )
        return json.encodeToString(payload)
    }

    private fun encodeEvent(event: NotificationSentEvent): String {
        val writer = SpecificDatumWriter(NotificationSentEvent::class.java)
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(NotificationSentEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }

    @Serializable
    private data class NotificationSentPayload(
        val notificationId: String,
        val orderId: String,
        val channel: String,
        val template: String,
        val payload: String,
        val status: String,
    )

    private companion object {
        val json = Json.Default
    }
}
