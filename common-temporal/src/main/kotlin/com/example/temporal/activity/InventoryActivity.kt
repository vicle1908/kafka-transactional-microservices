package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.UUID

@ActivityInterface
interface InventoryActivity {
    fun reserveInventory(orderId: UUID): InventoryReservationResult

    fun releaseInventory(orderId: UUID): InventoryReservationResult

    fun adjustStock(request: AdjustStockRequest): InventoryReservationResult

    fun getStockLevels(productIds: List<String>): InventoryStockLevel
}
