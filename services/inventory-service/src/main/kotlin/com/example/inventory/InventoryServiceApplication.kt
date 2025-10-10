package com.example.inventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "com.example.inventory",
        "com.example.outbox",
        "com.example.saga",
        "com.example.kafka",
        "com.example.persistence",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.example.inventory.domain",
        "com.example.persistence.outbox",
        "com.example.saga",
    ],
)
class InventoryServiceApplication

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}
