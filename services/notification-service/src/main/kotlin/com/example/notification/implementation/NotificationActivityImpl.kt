package com.example.notification.implementation

import com.example.temporal.activity.NotificationActivity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationActivityImpl : NotificationActivity {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendNotification(orderId: UUID) {
        logger.info("Dispatching notification for order {}", orderId)
    }
}
