package com.example.notification.application

import java.util.UUID

data class SendNotificationCommand(
    val orderId: UUID,
    val channel: String,
    val template: String,
    val recipient: String? = null,
    val payload: String = "{}",
)

data class SendNotificationResult(
    val success: Boolean,
    val notificationId: UUID? = null,
    val failureReason: String? = null,
)
