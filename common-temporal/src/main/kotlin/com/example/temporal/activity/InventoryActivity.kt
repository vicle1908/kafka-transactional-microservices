package com.example.temporal.activity

import com.example.inventory.proto.AdjustStockRequest
import io.temporal.activity.ActivityInterface
import java.util.List
import java.util.UUID

@ActivityInterface
interface InventoryActivity {
    fun reserveInventory(orderId: UUID): InventoryReservationResult

    fun releaseInventory(orderId: UUID): InventoryReservationResult

    fun adjustStock(request: AdjustStockRequest): InventoryReservationResult

    fun getStockLevels(productIds: List<String>): InventoryStockLevel
}
