package com.example.inventory.activity

import com.example.inventory.InventoryService
import com.example.inventory.proto.AdjustStockRequest
import com.example.temporal.activity.InventoryActivity
import com.example.temporal.activity.InventoryReservationResult
import com.example.temporal.activity.InventoryStockLevel
import com.example.temporal.activity.ReservedItem
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Implementation of InventoryActivity for Temporal workflow integration.
 * Bridges Temporal workflow activities with the inventory service domain logic.
 */
@Component
class InventoryActivityImpl(
    private val inventoryService: InventoryService,
    private val meterRegistry: MeterRegistry,
) : InventoryActivity {
    private val logger = LoggerFactory.getLogger(InventoryActivityImpl::class.java)

    private val inventoryReservedCounter: Counter =
        Counter
            .builder("temporal.activity.inventory.reserved")
            .description("Number of inventory reservation activities")
            .register(meterRegistry)

    private val inventoryReleasedCounter: Counter =
        Counter
            .builder("temporal.activity.inventory.released")
            .description("Number of inventory release activities")
            .register(meterRegistry)

    private val inventoryReservationTimer: Timer =
        Timer
            .builder("temporal.activity.inventory.reservation.duration")
            .description("Duration of inventory reservation activities")
            .register(meterRegistry)

    override fun reserveInventory(orderId: UUID): InventoryReservationResult {
        val startTime = Instant.now()
        logger.info("Reserving inventory for orderId: $orderId")

        return try {
            // Check current stock levels first
            val stockItems = inventoryService.getStockForOrder(orderId)

            if (stockItems.isEmpty()) {
                logger.warn("No stock items found for orderId: $orderId")
                return InventoryReservationResult(
                    success = false,
                    reservationId = null,
                    reservedItems = emptyList(),
                    message = "No stock items found for order",
                )
            }

            // Attempt to reserve stock
            val reservationResult = inventoryService.reserveStockForOrder(orderId)
            val duration = Duration.between(startTime, Instant.now()).toMillis()

            inventoryReservedCounter.increment()
            inventoryReservationTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            if (reservationResult.success) {
                logger.info(
                    "Inventory reserved successfully for orderId: $orderId, reservationId: ${reservationResult.reservationId}",
                )

                val reservedItems =
                    reservationResult.reservedItems.map { item ->
                        ReservedItem(
                            productId = item.productId,
                            quantity = item.quantity,
                            reservationId = item.reservationId,
                        )
                    }

                InventoryReservationResult(
                    success = true,
                    reservationId = reservationResult.reservationId,
                    reservedItems = reservedItems,
                    message = "Inventory reserved successfully",
                )
            } else {
                logger.error("Inventory reservation failed for orderId: $orderId")
                InventoryReservationResult(
                    success = false,
                    reservationId = null,
                    reservedItems = emptyList(),
                    message = "Inventory reservation failed - insufficient stock",
                )
            }
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now()).toMillis()
            logger.error("Error reserving inventory for orderId: $orderId", e)

            inventoryReservedCounter.increment()
            inventoryReservationTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            InventoryReservationResult(
                success = false,
                reservationId = null,
                reservedItems = emptyList(),
                message = "Error reserving inventory: ${e.message}",
            )
        }
    }

    override fun releaseInventory(orderId: UUID): InventoryReservationResult {
        val startTime = Instant.now()
        logger.info("Releasing inventory for orderId: $orderId")

        return try {
            inventoryService.releaseStockForOrder(orderId)
            val duration = Duration.between(startTime, Instant.now()).toMillis()

            inventoryReleasedCounter.increment()

            logger.info("Inventory released successfully for orderId: $orderId")
            InventoryReservationResult(
                success = true,
                reservationId = null,
                reservedItems = emptyList(),
                message = "Inventory released successfully",
            )
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now()).toMillis()
            logger.error("Error releasing inventory for orderId: $orderId", e)

            inventoryReleasedCounter.increment()

            InventoryReservationResult(
                success = false,
                reservationId = null,
                reservedItems = emptyList(),
                message = "Error releasing inventory: ${e.message}",
            )
        }
    }

    override fun adjustStock(request: AdjustStockRequest): InventoryReservationResult {
        val startTime = Instant.now()
        logger.info("Adjusting stock for orderId: ${request.orderId}")

        return try {
            for (adjustment in request.adjustmentsList) {
                inventoryService.adjustStock(
                    productId = adjustment.productId,
                    quantityAdjustment = adjustment.quantityAdjustment,
                    reason = adjustment.reason.name,
                )
            }

            val duration = Duration.between(startTime, Instant.now()).toMillis()

            logger.info("Stock adjusted successfully for orderId: ${request.orderId}")
            InventoryReservationResult(
                success = true,
                reservationId = null,
                reservedItems = emptyList(),
                message = "Stock adjusted successfully",
            )
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now()).toMillis()
            logger.error("Error adjusting stock for orderId: ${request.orderId}", e)

            InventoryReservationResult(
                success = false,
                reservationId = null,
                reservedItems = emptyList(),
                message = "Error adjusting stock: ${e.message}",
            )
        }
    }

    override fun getStockLevels(productIds: List<String>): InventoryStockLevel {
        val startTime = Instant.now()
        logger.info("Getting stock levels for ${productIds.size} products")

        return try {
            val stockItems = inventoryService.getCurrentStockLevels(productIds)

            val stockLevels =
                stockItems.map { stock ->
                    InventoryStockLevel(
                        productId = stock.productId,
                        availableQuantity = stock.quantityAvailable,
                        reservedQuantity = stock.quantityReserved,
                        totalQuantity = stock.quantityAvailable + stock.quantityReserved,
                        version = stock.version,
                    )
                }

            logger.info("Retrieved stock levels for ${stockLevels.size} products")

            // Return first stock level as representative (for simplicity)
            // In a real implementation, this might return a list or different structure
            stockLevels.firstOrNull() ?: InventoryStockLevel(
                productId = "",
                availableQuantity = 0,
                reservedQuantity = 0,
                totalQuantity = 0,
                version = 0L,
            )
        } catch (e: Exception) {
            logger.error("Error getting stock levels", e)
            InventoryStockLevel(
                productId = "",
                availableQuantity = 0,
                reservedQuantity = 0,
                totalQuantity = 0,
                version = 0L,
            )
        }
    }
}
