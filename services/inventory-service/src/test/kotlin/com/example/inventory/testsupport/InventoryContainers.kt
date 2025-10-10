package com.example.inventory.testsupport

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer

class InventoryPostgresContainer : PostgreSQLContainer<InventoryPostgresContainer>("postgres:15.3")

object InventoryContainers {
    val postgres: InventoryPostgresContainer by lazy {
        InventoryPostgresContainer().apply {
            withDatabaseName("inventory_service_test")
            withUsername("inventory")
            withPassword("inventory")
            withReuse(true)
            start()
        }
    }

    fun registerPostgres(registry: DynamicPropertyRegistry) {
        val container = postgres
        registry.add("spring.datasource.url") { container.jdbcUrl }
        registry.add("spring.datasource.username") { container.username }
        registry.add("spring.datasource.password") { container.password }
        registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        registry.add("spring.flyway.url") { container.jdbcUrl }
        registry.add("spring.flyway.user") { container.username }
        registry.add("spring.flyway.password") { container.password }
        registry.add("spring.flyway.database-type") { "postgresql" }
        registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }
        registry.add("spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation") { "true" }
    }
}
