package com.example.inventory.testsupport

import org.testcontainers.containers.PostgreSQLContainer

class InventoryPostgresContainer :
    PostgreSQLContainer<InventoryPostgresContainer>("postgres:16.4")

object InventoryContainers {
    val postgres: InventoryPostgresContainer =
        InventoryPostgresContainer().apply {
            withDatabaseName("inventory_service_test")
            withUsername("inventory")
            withPassword("inventory")
            withReuse(true)
        }
}
