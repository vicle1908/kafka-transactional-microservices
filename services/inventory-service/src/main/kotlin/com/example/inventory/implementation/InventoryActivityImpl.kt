package com.example.inventory.implementation

import com.example.temporal.activity.InventoryActivity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InventoryActivityImpl : InventoryActivity {
    override fun reserveInventory(orderId: UUID) {
        println("*** Reserving inventory for order: $orderId in inventory-service ***")
        // Real inventory logic will go here
    }
}
