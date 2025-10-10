package com.example.notification.config

import com.example.notification.application.NotificationDispatchException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
class NotificationRetryConfig {
    @Bean
    fun notificationRetryTemplate(
        @Value("\${notification.dispatch.retry.max-attempts:3}") maxAttempts: Int,
        @Value("\${notification.dispatch.retry.initial-delay:500}") initialDelay: Long,
        @Value("\${notification.dispatch.retry.max-delay:5000}") maxDelay: Long,
        @Value("\${notification.dispatch.retry.multiplier:2.0}") multiplier: Double,
    ): RetryTemplate =
        RetryTemplate
            .builder()
            .maxAttempts(maxAttempts)
            .exponentialBackoff(initialDelay, multiplier, maxDelay)
            .retryOn(NotificationDispatchException::class.java)
            .build()
}
