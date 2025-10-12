package com.example.payments.activity

import com.example.payments.PaymentService
import com.example.temporal.activity.RefundPaymentActivity
import com.example.temporal.activity.RefundResult
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

/**
 * Implementation of RefundPaymentActivity for Temporal workflow compensation.
 * Bridges Temporal workflow compensation activities with the payments service domain logic.
 */
@Component
class RefundPaymentActivityImpl(
    private val paymentService: PaymentService,
    private val meterRegistry: MeterRegistry
) : RefundPaymentActivity {

    private val logger = LoggerFactory.getLogger(RefundPaymentActivityImpl::class.java)

    private val refundProcessedCounter: Counter = Counter.builder("temporal.activity.refund.processed")
        .description("Number of refund processing activities")
        .register(meterRegistry)

    private val refundSuccessCounter: Counter = Counter.builder("temporal.activity.refund.success")
        .description("Number of successful refund activities")
        .register(meterRegistry)

    private val refundFailureCounter: Counter = Counter.builder("temporal.activity.refund.failure")
        .description("Number of failed refund activities")
        .register(meterRegistry)

    private val refundProcessingTimer: Timer = Timer.builder("temporal.activity.refund.duration")
        .description("Duration of refund processing activities")
        .register(meterRegistry)

    override fun refundPayment(orderId: UUID): RefundResult {
        val startTime = System.currentTimeMillis()
        logger.info("Processing refund for orderId: $orderId")

        return try {
            // Process refund using existing PaymentService compensation method
            val refundOutcome = paymentService.compensate(orderId, "Saga compensation via Temporal workflow")
            val duration = System.currentTimeMillis() - startTime

            refundProcessedCounter.increment()
            refundProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (refundOutcome.success) {
                refundSuccessCounter.increment()
                logger.info("Refund processed successfully for orderId: $orderId, refundId: ${refundOutcome.refundId}")

                RefundResult(
                    success = true,
                    refundId = refundOutcome.refundId,
                    amount = refundOutcome.amount,
                    currency = refundOutcome.currency,
                    message = "Refund processed successfully"
                )
            } else {
                refundFailureCounter.increment()
                logger.error("Refund processing failed for orderId: $orderId, reason: ${refundOutcome.failureReason}")

                RefundResult(
                    success = false,
                    refundId = null,
                    amount = refundOutcome.amount,
                    currency = refundOutcome.currency,
                    message = refundOutcome.failureReason ?: "Refund processing failed"
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error processing refund for orderId: $orderId", e)

            refundProcessedCounter.increment()
            refundFailureCounter.increment()
            refundProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            RefundResult(
                success = false,
                refundId = null,
                amount = null,
                currency = null,
                message = "Error processing refund: ${e.message}"
            )
        }
    }
}
