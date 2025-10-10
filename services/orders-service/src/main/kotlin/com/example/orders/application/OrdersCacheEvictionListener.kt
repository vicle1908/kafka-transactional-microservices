package com.example.orders.application

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrdersCacheEvictionListener(
    private val cacheManager: CacheManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderChanged(event: OrderChangedEvent) {
        cacheManager.getCache("orders:by-id")?.evict(event.orderId)
    }
}
