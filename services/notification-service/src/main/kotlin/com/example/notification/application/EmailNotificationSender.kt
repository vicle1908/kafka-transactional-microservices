package com.example.notification.application

import com.example.notification.config.NotificationChannelProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmailNotificationSender(
    private val channelProperties: NotificationChannelProperties,
) : NotificationChannelSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val channel: String = "email"

    override fun send(command: SendNotificationCommand) {
        val config = channelProperties.email
        if (!config.enabled) {
            throw NotificationDispatchException("Email channel disabled")
        }
        if (config.simulateFailure) {
            throw NotificationDispatchException("Simulated email provider failure")
        }
        if (command.payload.isBlank()) {
            throw NotificationDispatchException("Email payload must not be blank")
        }

        logger.info(
            "Dispatching email notification for order {} using template {}",
            command.orderId,
            command.template,
        )
    }
}
