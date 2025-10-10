package com.example.notification.application

import org.springframework.stereotype.Component

@Component
class NotificationSenderRegistry(
    senders: List<NotificationChannelSender>,
) {
    private val senderMap: Map<String, NotificationChannelSender> =
        buildMap {
            senders.forEach { sender ->
                put(sender.channel, sender)
            }
        }

    fun dispatch(command: SendNotificationCommand) {
        val sender =
            senderMap[command.channel]
                ?: throw NotificationDispatchException(
                    "Unsupported notification channel '${command.channel}'",
                )

        sender.send(command)
    }
}
