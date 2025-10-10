package com.example.notification.application

import com.example.notification.config.NotificationChannelProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SmsNotificationSender(
    private val channelProperties: NotificationChannelProperties,
) : NotificationChannelSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val channel: String = "sms"

    override fun send(command: SendNotificationCommand) {
        val config = channelProperties.sms
        if (!config.enabled) {
            throw NotificationDispatchException("SMS channel disabled")
        }
        if (config.simulateFailure) {
            throw NotificationDispatchException("Simulated SMS provider failure")
        }
        if (command.payload.isBlank()) {
            throw NotificationDispatchException("SMS payload must not be blank")
        }

        logger.info(
            "Dispatching SMS notification for order {} using template {}",
            command.orderId,
            command.template,
        )
    }
}
