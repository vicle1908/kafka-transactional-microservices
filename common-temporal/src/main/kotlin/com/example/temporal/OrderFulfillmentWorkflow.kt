package com.example.temporal

import com.example.inventory.proto.ReconcileStockRequest
import com.example.inventory.proto.StockAdjustment
import java.util.UUID

/**
 * Placeholder Temporal workflow definition capturing desired API. The real implementation will
 * be added when we wire Temporal workers in Phase 4 Task P4.7.
 */
interface OrderFulfillmentWorkflow {
    fun start(orderId: UUID)
}

data class PaymentCommand(
    val orderId: UUID,
    val amountCents: Long,
)

data class InventoryCommand(
    val orderId: UUID,
    val adjustments: List<StockAdjustment>,
)

data class NotificationCommand(
    val orderId: UUID,
    val channel: String,
    val template: String,
    val payload: String,
)

data class ReconcileRequest(
    val request: ReconcileStockRequest,
)
