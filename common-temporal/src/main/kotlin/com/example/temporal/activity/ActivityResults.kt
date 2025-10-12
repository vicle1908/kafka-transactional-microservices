package com.example.temporal.activity

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Shared result payloads and request DTOs used by Temporal activities.
 */
data class PaymentResult(
    val success: Boolean,
    val paymentId: UUID? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val processedAt: Instant? = null,
    val message: String? = null,
)

data class RefundResult(
    val success: Boolean,
    val refundId: UUID? = null,
    val refundedAt: Instant? = null,
    val message: String? = null,
)

data class InventoryReservationResult(
    val success: Boolean,
    val reservationId: UUID? = null,
    val reservedItems: List<ReservedItem> = emptyList(),
    val message: String? = null,
)

data class ReservedItem(
    val productId: String,
    val quantity: Int,
    val reservationId: UUID,
)

data class InventoryStockLevel(
    val productId: String,
    val availableQuantity: Int,
    val reservedQuantity: Int,
    val totalQuantity: Int,
    val version: Long,
)

data class NotificationResult(
    val success: Boolean,
    val notificationId: UUID? = null,
    val channel: String? = null,
    val recipient: String? = null,
    val sentAt: Instant? = null,
    val message: String? = null,
)

data class AdjustStockRequest(
    val orderId: UUID?,
    val adjustments: List<StockAdjustment>,
)

data class StockAdjustment(
    val productId: String,
    val quantityAdjustment: Int,
    val reason: String? = null,
)
