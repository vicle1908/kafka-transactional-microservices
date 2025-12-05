package com.example.payments.activity

import com.example.payments.application.PaymentService
import com.example.payments.application.ProcessPaymentCommand
import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.PaymentResult
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Implementation of PaymentActivity for Temporal workflow integration.
 * Bridges Temporal workflow activities with the payments service domain logic.
 */
@Component
@Suppress("TooGenericExceptionCaught", "LongMethod")
class PaymentActivityImpl(
    private val paymentService: PaymentService,
    private val meterRegistry: MeterRegistry,
) : PaymentActivity {
    private val logger = LoggerFactory.getLogger(PaymentActivityImpl::class.java)

    override fun getOrderAmount(orderId: UUID): java.math.BigDecimal? = paymentService.getOrderAmount(orderId)

    private val paymentProcessedCounter: Counter =
        Counter
            .builder("temporal.activity.payment.processed")
            .description("Number of payment processing activities")
            .register(meterRegistry)

    private val paymentSuccessCounter: Counter =
        Counter
            .builder("temporal.activity.payment.success")
            .description("Number of successful payment activities")
            .register(meterRegistry)

    private val paymentFailureCounter: Counter =
        Counter
            .builder("temporal.activity.payment.failure")
            .description("Number of failed payment activities")
            .register(meterRegistry)

    private val paymentProcessingTimer: Timer =
        Timer
            .builder("temporal.activity.payment.duration")
            .description("Duration of payment processing activities")
            .register(meterRegistry)

    override fun processPayment(
        orderId: UUID,
        amount: java.math.BigDecimal,
    ): PaymentResult {
        val startTime = System.currentTimeMillis()
        logger.info("Processing payment for orderId: $orderId")

        return try {
            val command =
                ProcessPaymentCommand(
                    orderId = orderId,
                    amount = amount,
                )
            val paymentOutcome = paymentService.handle(command)
            val duration = System.currentTimeMillis() - startTime

            paymentProcessedCounter.increment()
            paymentProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            when (paymentOutcome) {
                is com.example.payments.application.PaymentProcessingOutcome.Completed -> {
                    paymentSuccessCounter.increment()
                    val payment = paymentService.getPaymentById(paymentOutcome.paymentId)
                    logger.info(
                        "Payment processed successfully for orderId: $orderId, paymentId: ${paymentOutcome.paymentId}",
                    )

                    PaymentResult(
                        success = true,
                        paymentId = paymentOutcome.paymentId,
                        amount = payment?.amount,
                        currency = "USD",
                        processedAt = payment?.processedAt(),
                        message = "Payment processed successfully",
                    )
                }

                is com.example.payments.application.PaymentProcessingOutcome.Failed -> {
                    paymentFailureCounter.increment()
                    logger.error("Payment processing failed for orderId: $orderId, reason: ${paymentOutcome.reason}")

                    PaymentResult(
                        success = false,
                        paymentId = null,
                        amount = null,
                        currency = null,
                        message = paymentOutcome.reason,
                    )
                }

                is com.example.payments.application.PaymentProcessingOutcome.AlreadyProcessed -> {
                    paymentSuccessCounter.increment()
                    val payment = paymentService.getPaymentById(paymentOutcome.paymentId)
                    logger.info("Payment already processed for orderId: $orderId, paymentId: ${paymentOutcome.paymentId}")

                    PaymentResult(
                        success = true,
                        paymentId = paymentOutcome.paymentId,
                        amount = payment?.amount,
                        currency = "USD",
                        processedAt = payment?.processedAt(),
                        message = "Payment already processed",
                    )
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error processing payment for orderId: $orderId", e)

            paymentProcessedCounter.increment()
            paymentFailureCounter.increment()
            paymentProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            PaymentResult(
                success = false,
                paymentId = null,
                amount = null,
                currency = null,
                message = "Error processing payment: ${e.message}",
            )
        }
    }
}
