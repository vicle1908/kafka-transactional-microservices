package com.example.inventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "com.example.inventory",
        "com.example.saga",
        "com.example.kafka",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.example.inventory.domain",
        "com.example.outbox.repository",
        "com.example.saga",
    ],
)
@EntityScan(
    basePackages = [
        "com.example.inventory.domain",
        "com.example.outbox.entity",
        "com.example.saga",
    ],
)
class InventoryServiceApplication

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}
