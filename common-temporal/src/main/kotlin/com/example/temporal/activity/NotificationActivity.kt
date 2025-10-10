package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.UUID

@ActivityInterface
interface NotificationActivity {
    fun sendNotification(orderId: UUID)
}
