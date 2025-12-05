package com.example.inventory.query

import com.example.inventory.domain.InventoryStockEntity

data class InventoryStockDto(
    val sku: String,
    val availableQuantity: Int,
)

internal fun InventoryStockEntity.toDto(): InventoryStockDto =
    InventoryStockDto(
        sku = this.sku,
        availableQuantity = this.availableQuantity(),
    )
