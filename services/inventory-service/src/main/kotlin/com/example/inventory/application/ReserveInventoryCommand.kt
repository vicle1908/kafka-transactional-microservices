package com.example.inventory.application

import java.util.UUID

data class ReserveInventoryCommand(
    val orderId: UUID,
    val sku: String,
    val quantity: Int,
)
