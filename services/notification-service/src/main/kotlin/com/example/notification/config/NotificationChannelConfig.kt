package com.example.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(NotificationChannelProperties::class)
class NotificationChannelConfig

@ConfigurationProperties(prefix = "notification.channels")
class NotificationChannelProperties {
    var email: Channel = Channel()
    var sms: Channel = Channel()
    var push: Channel = Channel()

    class Channel {
        var enabled: Boolean = true
        var simulateFailure: Boolean = false
    }
}
