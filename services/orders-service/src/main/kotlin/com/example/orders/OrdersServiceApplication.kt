package com.example.orders

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.example.orders",
        "com.example.outbox",
        "com.example.saga",
        "com.example.kafka",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.example.orders.repository",
        "com.example.outbox.repository",
        "com.example.saga",
    ],
)
@EnableScheduling
class OrdersServiceApplication

fun main(args: Array<String>) {
    runApplication<OrdersServiceApplication>(*args)
}
