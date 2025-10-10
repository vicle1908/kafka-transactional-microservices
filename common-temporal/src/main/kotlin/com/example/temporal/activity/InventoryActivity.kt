package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.UUID

@ActivityInterface
interface InventoryActivity {
    fun reserveInventory(orderId: UUID)
}
