package com.example.notification.application

import com.example.notification.config.NotificationChannelProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PushNotificationSender(
    private val channelProperties: NotificationChannelProperties,
) : NotificationChannelSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val channel: String = "push"

    override fun send(command: SendNotificationCommand) {
        val config = channelProperties.push
        if (!config.enabled) {
            throw NotificationDispatchException("Push channel disabled")
        }
        if (config.simulateFailure) {
            throw NotificationDispatchException("Simulated push provider failure")
        }
        if (command.payload.isBlank()) {
            throw NotificationDispatchException("Push payload must not be blank")
        }

        logger.info(
            "Dispatching push notification for order {} using template {}",
            command.orderId,
            command.template,
        )
    }
}
