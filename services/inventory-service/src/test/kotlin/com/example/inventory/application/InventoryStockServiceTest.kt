package com.example.inventory.application

import com.example.inventory.InventoryServiceApplication
import com.example.inventory.domain.InventoryStockRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [InventoryServiceApplication::class])
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryStockServiceTest {
    @Autowired
    private lateinit var inventoryStockService: InventoryStockService

    @Autowired
    private lateinit var stockRepository: InventoryStockRepository

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
