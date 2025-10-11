package com.example.inventory.application

import com.example.inventory.application.port.input.StockReconciliationUseCase
import com.example.inventory.domain.InventoryStockEntity
import com.example.inventory.domain.InventoryStockRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryStockService(
    private val stockRepository: InventoryStockRepository,
) : StockReconciliationUseCase {
    @Transactional
    @CacheEvict(cacheNames = ["inventory:stock:by-sku"], key = "#command.sku")
    override fun reconcile(command: ReconcileInventoryCommand) {
        val stock =
            stockRepository
                .lockBySku(command.sku)
                ?.apply { reconcile(command.availableQuantity) }
                ?: InventoryStockEntity(
                    sku = command.sku,
                    availableQuantity = command.availableQuantity,
                )
        stockRepository.save(stock)
    }
}
