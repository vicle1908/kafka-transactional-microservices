package com.example.inventory.application

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** Published when stock for a SKU was changed within a transaction. */
data class InventoryStockChangedEvent(
    val sku: String,
)

@Component
class InventoryCacheEvictionListener(
    private val cacheManager: CacheManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onStockChanged(event: InventoryStockChangedEvent) {
        cacheManager.getCache("inventory:stock:by-sku")?.evict(event.sku)
    }
}
