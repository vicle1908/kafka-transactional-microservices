@file:Suppress("TooGenericExceptionCaught", "LongMethod", "LongParameterList", "ReturnCount")

package com.example.payments.application

import com.example.events.avro.PaymentCompletedEvent
import com.example.events.avro.PaymentFailedEvent
import com.example.events.avro.PaymentRefundFailedEvent
import com.example.events.avro.PaymentRefundedEvent
import com.example.observability.StructuredLogger
import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import com.example.outbox.repository.OutboxRepository
import com.example.payments.application.port.out.PaymentChargeRequest
import com.example.payments.application.port.out.PaymentChargeResult
import com.example.payments.application.port.out.PaymentChargeResult.Approved
import com.example.payments.application.port.out.PaymentChargeResult.Declined
import com.example.payments.application.port.out.PaymentGateway
import com.example.payments.application.port.out.RefundGateway
import com.example.payments.application.port.out.RefundRequest
import com.example.payments.application.port.out.RefundResult
import com.example.payments.client.OrdersGrpcClient
import com.example.payments.domain.PaymentEntity
import com.example.payments.domain.PaymentRepository
import com.example.payments.domain.PaymentStatus
import com.example.payments.domain.ProcessedEventEntity
import com.example.payments.domain.ProcessedEventRepository
import com.example.payments.domain.RefundEntity
import com.example.payments.domain.RefundRepository
import com.example.payments.domain.RefundStatus
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
@Suppress("LargeClass")
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
    private val processedEventRepository: ProcessedEventRepository,
    private val refundRepository: RefundRepository,
    private val refundGateway: RefundGateway,
    private val paymentGateway: PaymentGateway,
    private val ordersGrpcClient: OrdersGrpcClient,
) {
    private val logger = StructuredLogger.getLogger(PaymentService::class.java)

    @Transactional
    @Suppress("ReturnCount")
    fun handle(command: ProcessPaymentCommand): PaymentProcessingOutcome {
        logger.info(
            "Processing payment",
            "orderId" to command.orderId,
            "amount" to command.amount,
            "eventId" to command.eventId,
        )

        validate(command)
        findDuplicateOutcome(command)?.let { return it }

        val createdAt = Instant.now()
        val payment = createPendingPayment(command, createdAt)
        val chargeResult = executeCharge(payment.id ?: error("Payment id missing after save"), command)
        val outcome = applyChargeOutcome(payment, command.orderId, chargeResult)

        recordProcessedEvent(command.eventId)

        logger.info(
            "Payment processing completed",
            "paymentId" to payment.id,
            "outcome" to
                when (outcome) {
                    is PaymentProcessingOutcome.Completed -> "completed"
                    is PaymentProcessingOutcome.Failed -> "failed"
                    is PaymentProcessingOutcome.AlreadyProcessed -> "already_processed"
                },
        )

        return outcome
    }

    @Transactional
    fun compensate(
        orderId: UUID,
        reason: String?,
    ) {
        logger.info(
            "Starting payment compensation",
            "orderId" to orderId,
            "reason" to reason,
        )

        val occurredAt = Instant.now()
        initiateRefund(orderId, reason, occurredAt)

        logger.info(
            "Updating saga state for compensation",
            "orderId" to orderId,
        )

        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.COMPENSATING,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.IN_PROGRESS,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.PAYMENT_COMPENSATED, occurredAt)
                    },
                    occurredAt = occurredAt,
                ),
        )
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.FAILED,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.COMPENSATING,
                    dataTransformer = { existing ->
                        val failureLabel = buildFailureLabel(reason)
                        SagaStepFormatter.append(existing, failureLabel, occurredAt)
                    },
                    occurredAt = occurredAt,
                ),
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.PAYMENT_COMPENSATED,
            state = SagaStatus.FAILED,
        )

        logger.info(
            "Payment compensation completed",
            "orderId" to orderId,
        )
    }

    fun getOrderAmount(orderId: UUID): BigDecimal? {
        val amount = ordersGrpcClient.getOrderAmount(orderId)
        if (amount != null) {
            return amount
        }
        logger.warn(
            "Could not retrieve order amount via gRPC, falling back to local payment repository",
            "orderId" to orderId,
        )
        val existingPayment = paymentRepository.findTopByOrderIdOrderByProcessedAtDesc(orderId)
        return existingPayment?.amount
    }

    fun getPaymentById(paymentId: UUID?): PaymentEntity? {
        if (paymentId == null) return null
        return paymentRepository.findById(paymentId).orElse(null)
    }

    @Transactional
    fun processRefund(
        orderId: UUID,
        reason: String?,
    ): ProcessRefundResult {
        logger.info(
            "Processing refund via Temporal activity",
            "orderId" to orderId,
            "reason" to reason,
        )

        return try {
            val occurredAt = Instant.now()
            val payment = paymentRepository.findTopByOrderIdOrderByProcessedAtDesc(orderId)

            if (payment == null) {
                logger.warn("No payment found for orderId: $orderId")
                return ProcessRefundResult(
                    success = false,
                    message = "No payment found for order",
                )
            }

            val paymentId =
                payment.id ?: return ProcessRefundResult(
                    success = false,
                    message = "Payment ID missing",
                )

            if (payment.status() !in setOf(PaymentStatus.COMPLETED, PaymentStatus.REFUNDING, PaymentStatus.REFUNDED)) {
                logger.warn("Payment not in refundable state for orderId: $orderId, status: ${payment.status()}")
                return ProcessRefundResult(
                    success = false,
                    message = "Payment not in refundable state: ${payment.status()}",
                )
            }

            val existingRefund = refundRepository.findFirstByPaymentIdOrderByRequestedAtDesc(paymentId)

            if (existingRefund != null && existingRefund.status() == RefundStatus.COMPLETED) {
                logger.info("Refund already completed for orderId: $orderId")
                return ProcessRefundResult(
                    success = true,
                    refundId = existingRefund.id,
                    refundedAt = existingRefund.completedAt(),
                    message = "Refund already completed",
                )
            }

            initiateRefund(orderId, reason, occurredAt)

            val completedRefund = refundRepository.findFirstByPaymentIdOrderByRequestedAtDesc(paymentId)
            if (completedRefund != null && completedRefund.status() == RefundStatus.COMPLETED) {
                ProcessRefundResult(
                    success = true,
                    refundId = completedRefund.id,
                    refundedAt = completedRefund.completedAt(),
                    message = "Refund processed successfully",
                )
            } else {
                ProcessRefundResult(
                    success = false,
                    refundId = completedRefund?.id,
                    message = "Refund processing failed",
                )
            }
        } catch (e: Exception) {
            logger.error(
                "Error processing refund for orderId: $orderId",
                "error" to (e.message ?: "Unknown error"),
                "exception" to e.javaClass.simpleName,
            )
            ProcessRefundResult(
                success = false,
                message = "Error processing refund: ${e.message}",
            )
        }
    }

    private fun findDuplicateOutcome(command: ProcessPaymentCommand): PaymentProcessingOutcome? {
        val eventId = command.eventId
        if (eventId == null || !processedEventRepository.existsById(eventId)) {
            return null
        }
        logger.debug(
            "Event already processed, skipping",
            "eventId" to eventId,
        )
        val existing = paymentRepository.findTopByOrderIdOrderByProcessedAtDesc(command.orderId)
        return PaymentProcessingOutcome.AlreadyProcessed(existing?.id)
    }

    private fun createPendingPayment(
        command: ProcessPaymentCommand,
        createdAt: Instant,
    ): PaymentEntity {
        val payment =
            PaymentEntity(
                orderId = command.orderId,
                amount = command.amount,
                status = PaymentStatus.PENDING,
                processedAt = createdAt,
            )
        paymentRepository.save(payment)

        logger.info(
            "Payment entity created",
            "paymentId" to payment.id,
            "orderId" to command.orderId,
            "amount" to command.amount,
        )
        return payment
    }

    private fun executeCharge(
        paymentId: UUID,
        command: ProcessPaymentCommand,
    ): PaymentChargeResult {
        logger.info(
            "Initiating payment charge",
            "paymentId" to paymentId,
            "orderId" to command.orderId,
            "amount" to command.amount,
        )

        val result =
            paymentGateway.charge(
                PaymentChargeRequest(
                    paymentId = paymentId,
                    orderId = command.orderId,
                    amount = command.amount,
                    metadata = command.metadata,
                ),
            )

        logger.info(
            "Payment charge result received",
            "paymentId" to paymentId,
            "chargeResult" to
                when (result) {
                    is Approved -> "approved"
                    is Declined -> "declined"
                },
        )
        return result
    }

    private fun applyChargeOutcome(
        payment: PaymentEntity,
        orderId: UUID,
        chargeResult: PaymentChargeResult,
    ): PaymentProcessingOutcome =
        when (chargeResult) {
            is Approved -> recordSuccessfulCharge(payment, orderId, chargeResult)
            is Declined -> recordFailedCharge(payment, orderId, chargeResult.reason)
        }

    private fun recordProcessedEvent(eventId: UUID?) {
        eventId ?: return
        processedEventRepository.save(
            ProcessedEventEntity(
                eventId = eventId,
                processedAt = Instant.now(),
            ),
        )
    }

    private fun initiateRefund(
        orderId: UUID,
        reason: String?,
        occurredAt: Instant,
    ) {
        val payment =
            paymentRepository.findTopByOrderIdOrderByProcessedAtDesc(orderId)
                ?: return
        val paymentId = payment.id ?: return
        if (payment.status() !in setOf(PaymentStatus.COMPLETED, PaymentStatus.REFUNDING, PaymentStatus.REFUNDED)) {
            return
        }

        val existing =
            refundRepository.findFirstByPaymentIdOrderByRequestedAtDesc(paymentId)

        if (existing != null && existing.status() == RefundStatus.COMPLETED) {
            return
        }

        val normalizedReason = reason?.take(MAX_REFUND_REASON_LENGTH)

        val refund =
            existing
                ?: refundRepository.save(
                    RefundEntity(
                        paymentId = paymentId,
                        amount = payment.amount,
                        status = RefundStatus.PENDING,
                        requestedAt = occurredAt,
                        reason = normalizedReason,
                    ),
                )

        payment.markRefunding(occurredAt)
        paymentRepository.save(payment)

        val result =
            refundGateway.refund(
                RefundRequest(
                    refundId = refund.id ?: throw IllegalStateException("refund id missing"),
                    paymentId = paymentId,
                    orderId = orderId,
                    amount = payment.amount,
                    reason = normalizedReason,
                ),
            )

        when (result) {
            RefundResult.Completed -> {
                refund.markCompleted(occurredAt)
                refundRepository.save(refund)
                payment.markRefunded(occurredAt)
                paymentRepository.save(payment)

                val event =
                    PaymentRefundedEvent
                        .newBuilder()
                        .setEventId(UUID.randomUUID())
                        .setAggregateId(paymentId)
                        .setOccurredAt(occurredAt)
                        .setPayload(serializeRefundedPayload(payment, refund))
                        .build()
                persistOutbox(
                    aggregateType = "Payment",
                    aggregateId = paymentId.toString(),
                    eventType = "PaymentRefunded",
                    payload = encodeEvent(event),
                    occurredAt = occurredAt,
                )
            }

            is RefundResult.Failed -> {
                refund.markFailed(occurredAt, result.reason)
                refundRepository.save(refund)

                val failedEvent =
                    PaymentRefundFailedEvent
                        .newBuilder()
                        .setEventId(UUID.randomUUID())
                        .setAggregateId(paymentId)
                        .setOccurredAt(occurredAt)
                        .setPayload(serializeRefundFailedPayload(payment, refund))
                        .build()
                persistOutbox(
                    aggregateType = "Payment",
                    aggregateId = paymentId.toString(),
                    eventType = "PaymentRefundFailed",
                    payload = encodeEvent(failedEvent),
                    occurredAt = occurredAt,
                )
            }
        }
    }

    private fun validate(command: ProcessPaymentCommand) {
        require(command.amount > BigDecimal.ZERO) {
            "amount must be positive"
        }
    }

    private fun recordSuccessfulCharge(
        payment: PaymentEntity,
        orderId: UUID,
        chargeResult: Approved,
    ): PaymentProcessingOutcome.Completed {
        val completedAt = Instant.now()
        payment.markCompleted(completedAt)
        paymentRepository.save(payment)

        val event =
            PaymentCompletedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(payment.id!!)
                .setOccurredAt(completedAt)
                .setPayload(serializeCompletedPayload(payment, chargeResult))
                .build()
        persistOutbox(
            aggregateType = "Payment",
            aggregateId = payment.id!!.toString(),
            eventType = "PaymentCompleted",
            payload = encodeEvent(event),
            occurredAt = completedAt,
        )

        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.STARTED,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.PAYMENT_COMPLETED, completedAt)
                    },
                    occurredAt = completedAt,
                ),
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.PAYMENT_COMPLETED,
            state = SagaStatus.IN_PROGRESS,
        )

        return PaymentProcessingOutcome.Completed(payment.id!!)
    }

    private fun recordFailedCharge(
        payment: PaymentEntity,
        orderId: UUID,
        reason: String,
    ): PaymentProcessingOutcome.Failed {
        val failedAt = Instant.now()
        payment.markFailed(failedAt, reason)
        paymentRepository.save(payment)

        val event =
            PaymentFailedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(payment.id!!)
                .setOccurredAt(failedAt)
                .setPayload(serializeFailedPayload(payment))
                .build()
        persistOutbox(
            aggregateType = "Payment",
            aggregateId = payment.id!!.toString(),
            eventType = "PaymentFailed",
            payload = encodeEvent(event),
            occurredAt = failedAt,
        )

        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.FAILED,
            options =
                SagaTransitionOptions(
                    expectedState = SagaStatus.STARTED,
                    dataTransformer = { existing ->
                        SagaStepFormatter.append(existing, SagaStepNames.PAYMENT_FAILED, failedAt)
                    },
                    occurredAt = failedAt,
                ),
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.PAYMENT_FAILED,
            state = SagaStatus.FAILED,
        )

        return PaymentProcessingOutcome.Failed(payment.id!!, reason)
    }

    private fun persistOutbox(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: String,
        occurredAt: Instant,
    ) {
        outboxRepository.save(
            OutboxMessage(
                aggregateId = aggregateId,
                aggregateType = aggregateType,
                eventType = eventType,
                payload = payload,
                headers = null,
                status = OutboxStatus.PENDING,
                occurredAt = occurredAt,
            ),
        )
    }

    private fun serializeCompletedPayload(
        payment: PaymentEntity,
        chargeResult: Approved,
    ): String {
        val payload =
            PaymentCompletedPayload(
                paymentId = payment.id!!.toString(),
                orderId = payment.orderId.toString(),
                amount = payment.amount.toPlainString(),
                status = payment.status().name,
                confirmationCode = chargeResult.confirmationCode,
            )
        return json.encodeToString(payload)
    }

    private fun serializeFailedPayload(payment: PaymentEntity): String {
        val payload =
            PaymentFailedPayload(
                paymentId = payment.id!!.toString(),
                orderId = payment.orderId.toString(),
                amount = payment.amount.toPlainString(),
                status = payment.status().name,
                failureReason = payment.failureReason(),
            )
        return json.encodeToString(payload)
    }

    private fun encodeEvent(event: PaymentCompletedEvent): String = encodeAvro(event)

    private fun encodeEvent(event: PaymentFailedEvent): String = encodeAvro(event)

    private fun encodeEvent(event: PaymentRefundedEvent): String = encodeAvro(event)

    private fun encodeEvent(event: PaymentRefundFailedEvent): String = encodeAvro(event)

    private fun serializeRefundedPayload(
        payment: PaymentEntity,
        refund: RefundEntity,
    ): String {
        val payload =
            PaymentRefundedPayload(
                refundId = refund.id?.toString() ?: throw IllegalStateException("refund id missing"),
                paymentId = payment.id!!.toString(),
                orderId = payment.orderId.toString(),
                amount = payment.amount.toPlainString(),
                status = payment.status().name,
                reason = refund.reason,
            )
        return json.encodeToString(payload)
    }

    private fun serializeRefundFailedPayload(
        payment: PaymentEntity,
        refund: RefundEntity,
    ): String {
        val payload =
            PaymentRefundFailedPayload(
                refundId = refund.id?.toString() ?: throw IllegalStateException("refund id missing"),
                paymentId = payment.id!!.toString(),
                orderId = payment.orderId.toString(),
                amount = payment.amount.toPlainString(),
                status = payment.status().name,
                failureReason = refund.failureReason(),
            )
        return json.encodeToString(payload)
    }

    private fun <T> encodeAvro(event: T): String where T : org.apache.avro.specific.SpecificRecord {
        val schema = event.schema
        val writer = SpecificDatumWriter<T>(schema)
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(schema, output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }

    @Serializable
    private data class PaymentCompletedPayload(
        val paymentId: String,
        val orderId: String,
        val amount: String,
        val status: String,
        val confirmationCode: String,
    )

    @Serializable
    private data class PaymentFailedPayload(
        val paymentId: String,
        val orderId: String,
        val amount: String,
        val status: String,
        val failureReason: String?,
    )

    @Serializable
    private data class PaymentRefundedPayload(
        val refundId: String,
        val paymentId: String,
        val orderId: String,
        val amount: String,
        val status: String,
        val reason: String?,
    )

    @Serializable
    private data class PaymentRefundFailedPayload(
        val refundId: String,
        val paymentId: String,
        val orderId: String,
        val amount: String,
        val status: String,
        val failureReason: String?,
    )

    private companion object {
        val json = Json.Default

        private fun buildFailureLabel(reason: String?): String =
            if (reason.isNullOrBlank()) {
                SagaStepNames.PAYMENT_COMPENSATED
            } else {
                "${SagaStepNames.PAYMENT_COMPENSATED}:$reason"
            }

        private const val MAX_REFUND_REASON_LENGTH = 1024
    }
}

sealed interface PaymentProcessingOutcome {
    data class Completed(
        val paymentId: UUID,
    ) : PaymentProcessingOutcome

    data class Failed(
        val paymentId: UUID,
        val reason: String,
    ) : PaymentProcessingOutcome

    data class AlreadyProcessed(
        val paymentId: UUID?,
    ) : PaymentProcessingOutcome
}

data class ProcessRefundResult(
    val success: Boolean,
    val refundId: UUID? = null,
    val refundedAt: java.time.Instant? = null,
    val message: String? = null,
)
