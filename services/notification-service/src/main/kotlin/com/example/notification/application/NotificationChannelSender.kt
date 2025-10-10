package com.example.notification.application

interface NotificationChannelSender {
    val channel: String

    fun send(command: SendNotificationCommand)
}
