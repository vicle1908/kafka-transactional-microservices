@file:Suppress("MaxLineLength")

package com.example.notifications.activity

import com.example.notifications.NotificationService
import com.example.temporal.activity.NotificationActivity
import com.example.temporal.activity.NotificationResult
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Implementation of NotificationActivity for Temporal workflow integration.
 * Bridges Temporal workflow activities with the notifications service domain logic.
 */
@Component
@Suppress("TooGenericExceptionCaught")
class NotificationActivityImpl(
    private val notificationService: NotificationService,
    private val meterRegistry: MeterRegistry,
) : NotificationActivity {
    private val logger = LoggerFactory.getLogger(NotificationActivityImpl::class.java)

    private val notificationSentCounter: Counter =
        Counter
            .builder("temporal.activity.notification.sent")
            .description("Number of notification sending activities")
            .register(meterRegistry)

    private val notificationSuccessCounter: Counter =
        Counter
            .builder("temporal.activity.notification.success")
            .description("Number of successful notification activities")
            .register(meterRegistry)

    private val notificationFailureCounter: Counter =
        Counter
            .builder("temporal.activity.notification.failure")
            .description("Number of failed notification activities")
            .register(meterRegistry)

    private val notificationProcessingTimer: Timer =
        Timer
            .builder("temporal.activity.notification.duration")
            .description("Duration of notification processing activities")
            .register(meterRegistry)

    override fun sendOrderConfirmation(
        orderId: UUID,
        customerEmail: String,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info("Sending order confirmation for orderId: $orderId to email: $customerEmail")

        return try {
            val notificationOutcome = notificationService.sendOrderConfirmation(orderId, customerEmail)
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Order confirmation sent successfully for orderId: $orderId, notificationId: ${notificationOutcome.notificationId}",
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Order confirmation sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send order confirmation for orderId: $orderId, reason: ${notificationOutcome.failureReason}",
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send order confirmation",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending order confirmation for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending order confirmation: ${e.message}",
            )
        }
    }

    override fun sendPaymentConfirmation(
        orderId: UUID,
        customerPhone: String,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info("Sending payment confirmation for orderId: $orderId to phone: $customerPhone")

        return try {
            val notificationOutcome = notificationService.sendPaymentConfirmation(orderId, customerPhone)
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Payment confirmation sent successfully for orderId: $orderId, notificationId: ${notificationOutcome.notificationId}",
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Payment confirmation sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send payment confirmation for orderId: $orderId, reason: ${notificationOutcome.failureReason}",
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send payment confirmation",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending payment confirmation for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending payment confirmation: ${e.message}",
            )
        }
    }

    override fun sendShippingConfirmation(
        orderId: UUID,
        customerDeviceToken: String,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info("Sending shipping confirmation for orderId: $orderId to device: $customerDeviceToken")

        return try {
            val notificationOutcome = notificationService.sendShippingConfirmation(orderId, customerDeviceToken)
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Shipping confirmation sent successfully for orderId: $orderId, notificationId: ${notificationOutcome.notificationId}",
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Shipping confirmation sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send shipping confirmation for orderId: $orderId, reason: ${notificationOutcome.failureReason}",
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send shipping confirmation",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending shipping confirmation for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending shipping confirmation: ${e.message}",
            )
        }
    }

    override fun sendOrderCancellation(
        orderId: UUID,
        customerEmail: String,
        cancellationReason: String,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info(
            "Sending order cancellation for orderId: $orderId to email: $customerEmail, reason: $cancellationReason",
        )

        return try {
            val notificationOutcome =
                notificationService.sendOrderCancellation(
                    orderId,
                    customerEmail,
                    cancellationReason,
                )
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Order cancellation sent successfully for orderId: $orderId, notificationId: ${notificationOutcome.notificationId}",
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Order cancellation sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send order cancellation for orderId: $orderId, reason: ${notificationOutcome.failureReason}",
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send order cancellation",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending order cancellation for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending order cancellation: ${e.message}",
            )
        }
    }

    override fun sendRefundConfirmation(
        orderId: UUID,
        customerEmail: String,
        refundAmount: Long,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info(
            "Sending refund confirmation for orderId={}, email={}, amount={}",
            orderId,
            customerEmail,
            refundAmount,
        )

        return try {
            val notificationOutcome = notificationService.sendRefundConfirmation(orderId, customerEmail, refundAmount)
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Refund confirmation sent successfully for orderId={}, notificationId={}",
                    orderId,
                    notificationOutcome.notificationId,
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Refund confirmation sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send refund confirmation for orderId={}, reason={}",
                    orderId,
                    notificationOutcome.failureReason,
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send refund confirmation",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending refund confirmation for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending refund confirmation: ${e.message}",
            )
        }
    }

    override fun sendCustomNotification(
        orderId: UUID,
        recipient: String,
        channel: String,
        template: String,
        payload: String,
    ): NotificationResult {
        val startTime = System.currentTimeMillis()
        logger.info(
            "Sending custom notification for orderId={}, recipient={}, channel={}",
            orderId,
            recipient,
            channel,
        )

        return try {
            val notificationOutcome =
                notificationService.sendCustomNotification(
                    orderId,
                    recipient,
                    channel,
                    template,
                    payload,
                )
            val duration = System.currentTimeMillis() - startTime

            notificationSentCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (notificationOutcome.success) {
                notificationSuccessCounter.increment()
                logger.info(
                    "Custom notification sent successfully for orderId={}, notificationId={}",
                    orderId,
                    notificationOutcome.notificationId,
                )

                NotificationResult(
                    success = true,
                    notificationId = notificationOutcome.notificationId,
                    message = "Custom notification sent successfully",
                )
            } else {
                notificationFailureCounter.increment()
                logger.error(
                    "Failed to send custom notification for orderId={}, reason={}",
                    orderId,
                    notificationOutcome.failureReason,
                )

                NotificationResult(
                    success = false,
                    notificationId = null,
                    message = notificationOutcome.failureReason ?: "Failed to send custom notification",
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Error sending custom notification for orderId: $orderId", e)

            notificationSentCounter.increment()
            notificationFailureCounter.increment()
            notificationProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            NotificationResult(
                success = false,
                notificationId = null,
                message = "Error sending custom notification: ${e.message}",
            )
        }
    }
}
