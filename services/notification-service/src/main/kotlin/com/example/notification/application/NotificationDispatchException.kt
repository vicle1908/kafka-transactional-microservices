package com.example.notification.application

class NotificationDispatchException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
