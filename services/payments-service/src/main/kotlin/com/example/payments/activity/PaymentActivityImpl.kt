package com.example.payments.activity

import com.example.payments.PaymentService
import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.PaymentResult
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

/**
 * Implementation of PaymentActivity for Temporal workflow integration.
 * Bridges Temporal workflow activities with the payments service domain logic.
 */
@Component
class PaymentActivityImpl(
    private val paymentService: PaymentService,
    private val meterRegistry: MeterRegistry
) : PaymentActivity {

    private val logger = LoggerFactory.getLogger(PaymentActivityImpl::class.java)

    private val paymentProcessedCounter: Counter = Counter.builder("temporal.activity.payment.processed")
        .description("Number of payment processing activities")
        .register(meterRegistry)

    private val paymentSuccessCounter: Counter = Counter.builder("temporal.activity.payment.success")
        .description("Number of successful payment activities")
        .register(meterRegistry)

    private val paymentFailureCounter: Counter = Counter.builder("temporal.activity.payment.failure")
        .description("Number of failed payment activities")
        .register(meterRegistry)

    private val paymentProcessingTimer: Timer = Timer.builder("temporal.activity.payment.duration")
        .description("Duration of payment processing activities")
        .register(meterRegistry)

    override fun processPayment(orderId: UUID): PaymentResult {
        val startTime = System.currentTimeMillis()
        logger.info("Processing payment for orderId: $orderId")

        return try {
            // Process payment using existing PaymentService
            val paymentOutcome = paymentService.processPayment(orderId)
            val duration = System.currentTimeMillis() - startTime

            paymentProcessedCounter.increment()
            paymentProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (paymentOutcome.success) {
                paymentSuccessCounter.increment()
                logger.info("Payment processed successfully for orderId: $orderId, paymentId: ${paymentOutcome.paymentId}")

                PaymentResult(
                    success = true,
                    paymentId = paymentOutcome.paymentId,
                    amount = paymentOutcome.amount,
                    currency = paymentOutcome.currency,
                    message = "Payment processed successfully"
                )
            } else {
                paymentFailureCounter.increment()
                logger.error("Payment processing failed for orderId: $orderId, reason: ${paymentOutcome.failureReason}")

                PaymentResult(
                    success = false,
                    paymentId = null,
                    amount = paymentOutcome.amount,
                    currency = paymentOutcome.currency,
                    message = paymentOutcome.failureReason ?: "Payment processing failed"
                )
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
                message = "Error processing payment: ${e.message}"
            )
        }
    }
}