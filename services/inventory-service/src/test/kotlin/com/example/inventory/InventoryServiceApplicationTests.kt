package com.example.inventory

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceApplicationTests {
    @Test
    fun contextLoads() {
        // Placeholder sanity check; real tests will cover inventory workflows.
    }
}
