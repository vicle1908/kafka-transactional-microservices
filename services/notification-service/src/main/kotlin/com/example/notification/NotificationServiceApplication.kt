package com.example.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.example.notification",
        "com.example.outbox",
        "com.example.saga",
        "com.example.kafka",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.example.notification.domain",
        "com.example.outbox.repository",
        "com.example.saga.repository",
    ],
)
@EnableKafka
@EnableScheduling
class NotificationServiceApplication

fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}
