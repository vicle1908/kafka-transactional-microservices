package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.*

@ActivityInterface
interface NotificationActivity {
    fun sendOrderConfirmation(orderId: UUID, customerEmail: String): NotificationResult
    fun sendPaymentConfirmation(orderId: UUID, customerPhone: String): NotificationResult
    fun sendShippingConfirmation(orderId: UUID, customerDeviceToken: String): NotificationResult
    fun sendOrderCancellation(orderId: UUID, customerEmail: String, cancellationReason: String): NotificationResult
    fun sendRefundConfirmation(orderId: UUID, customerEmail: String, refundAmount: Long): NotificationResult
    fun sendCustomNotification(orderId: UUID, recipient: String, channel: String, template: String, payload: String): NotificationResult
}
