package com.example.temporal.activity

import java.time.Instant
import java.util.List
import java.util.UUID

/**
 * Result data classes for Temporal activities.
 * These provide structured return types for activity methods.
 */

data class PaymentResult(
    val success: Boolean,
    val paymentId: UUID?,
    val amountCents: Long,
    val currency: String,
    val processedAt: Instant?,
    val message: String,
)

data class RefundResult(
    val success: Boolean,
    val refundId: UUID?,
    val refundedAt: Instant?,
    val message: String,
)

data class InventoryReservationResult(
    val success: Boolean,
    val reservationId: UUID?,
    val reservedItems: List<ReservedItem>,
    val message: String,
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
    val notificationId: UUID?,
    val channel: String,
    val recipient: String,
    val sentAt: Instant?,
    val message: String,
)
