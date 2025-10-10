package com.example.payments.application

import com.example.outbox.repository.OutboxRepository
import com.example.payments.PaymentsServiceApplication
import com.example.payments.application.PaymentProcessingOutcome
import com.example.payments.application.port.out.RefundGateway
import com.example.payments.application.port.out.RefundRequest
import com.example.payments.application.port.out.RefundResult
import com.example.payments.domain.PaymentRepository
import com.example.payments.domain.PaymentStatus
import com.example.payments.domain.ProcessedEventRepository
import com.example.payments.domain.RefundRepository
import com.example.payments.domain.RefundStatus
import com.example.saga.InvalidSagaStateTransitionException
import com.example.saga.SagaNames
import com.example.saga.SagaStateRepository
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    classes = [PaymentsServiceApplication::class],
    properties = ["spring.kafka.listener.auto-startup=false"],
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentServiceTest {
    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var processedEventRepository: ProcessedEventRepository

    @Autowired
    private lateinit var refundRepository: RefundRepository

    @MockBean
    private lateinit var refundGateway: RefundGateway

    private val json = Json { ignoreUnknownKeys = false }

    @BeforeEach
    fun cleanRepositories() {
        refundRepository.deleteAll()
        paymentRepository.deleteAll()
        outboxRepository.deleteAll()
        sagaStateRepository.deleteAll()
        processedEventRepository.deleteAll()
        given(refundGateway.refund(anyRefundRequest())).willReturn(RefundResult.Completed)
    }

    @Test
    @Transactional
    fun `handle should persist payment and append outbox event`() {
        val counterBefore = metricCount(SagaStepNames.PAYMENT_COMPLETED, SagaStatus.IN_PROGRESS)

        val orderId = UUID.randomUUID()
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, Instant.parse("2025-01-01T00:00:00Z")),
            at = Instant.parse("2025-01-01T00:00:00Z"),
        )

        val command =
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("49.95"),
                eventId = UUID.randomUUID(),
            )

        val outcome = paymentService.handle(command)
        assertThat(outcome).isInstanceOf(PaymentProcessingOutcome.Completed::class.java)
        val paymentId = (outcome as PaymentProcessingOutcome.Completed).paymentId

        val payments = paymentRepository.findAll()
        val events = outboxRepository.findAll()

        assertThat(paymentId).isNotNull
        assertThat(payments).hasSize(1)
        assertThat(events).hasSize(1)

        val storedEvent = events.first()
        val envelope = json.parseToJsonElement(storedEvent.payload).jsonObject
        val payload = json.parseToJsonElement(envelope["payload"]!!.jsonPrimitive.content).jsonObject

        assertThat(envelope["aggregate_id"]!!.jsonPrimitive.content).isEqualTo(paymentId.toString())
        assertThat(payload["paymentId"]!!.jsonPrimitive.content).isEqualTo(paymentId.toString())
        assertThat(payload["orderId"]!!.jsonPrimitive.content).isEqualTo(command.orderId.toString())
        assertThat(payload["amount"]!!.jsonPrimitive.content).isEqualTo("49.95")
        assertThat(payload["status"]!!.jsonPrimitive.content).isEqualTo("COMPLETED")
        assertThat(payload["confirmationCode"]!!.jsonPrimitive.content).startsWith("CONF-")

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)
        assertThat(saga.data()).contains(SagaStepNames.PAYMENT_COMPLETED)

        assertThat(processedEventRepository.count()).isEqualTo(1)

        val counterAfter = metricCount(SagaStepNames.PAYMENT_COMPLETED, SagaStatus.IN_PROGRESS)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
    }

    @Test
    fun `handle should reject non positive amount`() {
        val orderId = UUID.randomUUID()
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, Instant.parse("2025-01-01T00:00:00Z")),
            at = Instant.parse("2025-01-01T00:00:00Z"),
        )

        val command =
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal.ZERO,
            )

        assertThatThrownBy { paymentService.handle(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount")

        assertThat(paymentRepository.count()).isZero()
        assertThat(outboxRepository.count()).isZero()
        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.STARTED)
    }

    @Test
    fun `handle should record failure when gateway declines`() {
        val counterBefore = metricCount(SagaStepNames.PAYMENT_FAILED, SagaStatus.FAILED)

        val orderId = UUID.randomUUID()
        val createdAt = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, createdAt),
            at = createdAt,
        )

        val command =
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("5000.00"),
                eventId = UUID.randomUUID(),
            )

        val outcome = paymentService.handle(command)
        assertThat(outcome).isInstanceOf(PaymentProcessingOutcome.Failed::class.java)

        val payments = paymentRepository.findAll()
        val events = outboxRepository.findAll()

        assertThat(payments).hasSize(1)
        assertThat(events).hasSize(1)
        assertThat(events.first().eventType).isEqualTo("PaymentFailed")

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.data()).contains(SagaStepNames.PAYMENT_FAILED)

        assertThat(processedEventRepository.count()).isEqualTo(1)

        val counterAfter = metricCount(SagaStepNames.PAYMENT_FAILED, SagaStatus.FAILED)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
    }

    @Test
    fun `handle should skip duplicate event ids`() {
        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )

        val eventId = UUID.randomUUID()
        paymentService.handle(
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("25.00"),
                eventId = eventId,
            ),
        )

        val outcome =
            paymentService.handle(
                ProcessPaymentCommand(
                    orderId = orderId,
                    amount = BigDecimal("25.00"),
                    eventId = eventId,
                ),
            )

        assertThat(outcome).isInstanceOf(PaymentProcessingOutcome.AlreadyProcessed::class.java)
        assertThat(paymentRepository.count()).isEqualTo(1)
        assertThat(processedEventRepository.count()).isEqualTo(1)
    }

    @Test
    fun `compensate should move saga to compensating then failed`() {
        val counterBefore = metricCount(SagaStepNames.PAYMENT_COMPENSATED, SagaStatus.FAILED)

        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        paymentService.handle(
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("10"),
            ),
        )

        paymentService.compensate(orderId, "declined")

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.data()).contains(SagaStepNames.PAYMENT_COMPENSATED)
        assertThat(saga.data()).contains("declined")

        val payments = paymentRepository.findAll()
        assertThat(payments).hasSize(1)
        assertThat(payments.first().status()).isEqualTo(PaymentStatus.REFUNDED)

        val refunds = refundRepository.findAll()
        assertThat(refunds).hasSize(1)
        assertThat(refunds.first().status()).isEqualTo(RefundStatus.COMPLETED)

        val outboxEvents = outboxRepository.findAll()
        assertThat(outboxEvents).hasSize(2)
        val refundEvent = outboxEvents.last()
        assertThat(refundEvent.eventType).isEqualTo("PaymentRefunded")
        val refundEnvelope = json.parseToJsonElement(refundEvent.payload).jsonObject
        val refundPayload = json.parseToJsonElement(refundEnvelope["payload"]!!.jsonPrimitive.content).jsonObject
        assertThat(refundPayload["reason"]!!.jsonPrimitive.content).contains("declined")
        assertThat(refundPayload["status"]!!.jsonPrimitive.content).isEqualTo("REFUNDED")

        val counterAfter = metricCount(SagaStepNames.PAYMENT_COMPENSATED, SagaStatus.FAILED)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
    }

    @Test
    fun `compensate should emit refund failed event when gateway declines`() {
        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        val outcome =
            paymentService.handle(
                ProcessPaymentCommand(
                    orderId = orderId,
                    amount = BigDecimal("18.50"),
                ),
            ) as PaymentProcessingOutcome.Completed

        given(refundGateway.refund(anyRefundRequest())).willReturn(RefundResult.Failed("gateway-failure"))

        paymentService.compensate(orderId, "customer-cancelled")

        val payment = paymentRepository.findById(outcome.paymentId).orElseThrow()
        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDING)

        val refund = refundRepository.findAll().single()
        assertThat(refund.status()).isEqualTo(RefundStatus.FAILED)
        assertThat(refund.failureReason()).contains("gateway-failure")

        val refundFailedEvents =
            outboxRepository.findAll().filter { it.eventType == "PaymentRefundFailed" }
        assertThat(refundFailedEvents).hasSize(1)
        val payload = json.parseToJsonElement(refundFailedEvents.first().payload).jsonObject
        val body = json.parseToJsonElement(payload["payload"]!!.jsonPrimitive.content).jsonObject
        assertThat(body["reason"]!!.jsonPrimitive.content).contains("gateway-failure")
        assertThat(body["status"]!!.jsonPrimitive.content).isEqualTo("REFUNDING")
    }

    @Test
    fun `compensate should be idempotent for refunds`() {
        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        paymentService.handle(
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("15.00"),
            ),
        )

        paymentService.compensate(orderId, "duplicate")
        assertThatThrownBy { paymentService.compensate(orderId, "duplicate") }
            .isInstanceOf(InvalidSagaStateTransitionException::class.java)

        assertThat(refundRepository.count()).isEqualTo(1)
        val refundStatuses = refundRepository.findAll().map { it.status() }
        assertThat(refundStatuses).containsOnly(RefundStatus.COMPLETED)

        val refundEventCount =
            outboxRepository.findAll().count { it.eventType == "PaymentRefunded" }
        assertThat(refundEventCount).isEqualTo(1)
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

    @Suppress("UNCHECKED_CAST")
    private fun anyRefundRequest(): RefundRequest = any(RefundRequest::class.java) as RefundRequest
}
