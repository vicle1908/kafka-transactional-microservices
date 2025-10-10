package com.example.notification.application

import java.util.UUID

data class SendNotificationCommand(
    val orderId: UUID,
    val channel: String,
    val template: String,
    val payload: String,
)
