package com.example.inventory

import com.example.inventory.testsupport.InventoryContainers
import com.example.inventory.testsupport.InventoryFlywayTestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
@Import(InventoryFlywayTestConfig::class)
class InventoryServiceApplicationTests {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun register(registry: DynamicPropertyRegistry) {
            InventoryContainers.registerPostgres(registry)
        }
    }

    @Test
    fun contextLoads() {
        // Placeholder sanity check; real tests will cover inventory workflows.
    }
}
