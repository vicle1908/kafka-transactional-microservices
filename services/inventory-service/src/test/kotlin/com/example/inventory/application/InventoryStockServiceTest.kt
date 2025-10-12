package com.example.inventory.application

import com.example.inventory.InventoryServiceApplication
import com.example.inventory.domain.InventoryStockRepository
import com.example.inventory.testsupport.InventoryContainers
import com.example.inventory.testsupport.InventoryFlywayTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(classes = [InventoryServiceApplication::class])
@ActiveProfiles("test")
@Import(InventoryFlywayTestConfig::class, com.example.inventory.testsupport.TestCacheConfig::class)
class InventoryStockServiceTest {
    @Autowired
    private lateinit var inventoryStockService: InventoryStockService

    @Autowired
    private lateinit var stockRepository: InventoryStockRepository

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun register(registry: DynamicPropertyRegistry) {
            InventoryContainers.registerPostgres(registry)
        }
    }

    @BeforeEach
    fun clean() {
        stockRepository.deleteAll()
    }

    @Test
    fun `reconcile should create new stock when none exists`() {
        inventoryStockService.reconcile(
            ReconcileInventoryCommand(
                sku = "sku-new",
                availableQuantity = 15,
            ),
        )

        val stock = stockRepository.findBySku("sku-new")
        assertThat(stock).isNotNull
        assertThat(stock!!.availableQuantity()).isEqualTo(15)
    }

    @Test
    fun `reconcile should update existing stock`() {
        inventoryStockService.reconcile(
            ReconcileInventoryCommand(
                sku = "sku-existing",
                availableQuantity = 5,
            ),
        )

        inventoryStockService.reconcile(
            ReconcileInventoryCommand(
                sku = "sku-existing",
                availableQuantity = 20,
            ),
        )

        val stock = stockRepository.findBySku("sku-existing")
        assertThat(stock).isNotNull
        assertThat(stock!!.availableQuantity()).isEqualTo(20)
    }
}
