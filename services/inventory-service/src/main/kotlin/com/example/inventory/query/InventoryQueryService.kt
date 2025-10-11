package com.example.inventory.query

import com.example.inventory.domain.InventoryStockRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryQueryService(
    private val stockRepository: InventoryStockRepository,
) {
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["inventory:stock:by-sku"], key = "#sku")
    fun getStockBySku(sku: String): InventoryStockDto? = stockRepository.findBySku(sku)?.toDto()
}
