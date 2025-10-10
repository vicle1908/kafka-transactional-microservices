package com.example.inventory.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_items")
class InventoryItem(
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: String,
    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0,
    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Int = 0,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    fun reserve(quantity: Int) {
        if (availableQuantity < quantity) {
            throw InsufficientInventoryException(
                "Insufficient inventory for product $productId. Available: $availableQuantity, Requested: $quantity",
            )
        }

        availableQuantity -= quantity
        reservedQuantity += quantity
        updatedAt = Instant.now()
    }

    fun release(quantity: Int) {
        if (reservedQuantity < quantity) {
            throw InvalidOperationException(
                "Cannot release more than reserved quantity for product $productId. Reserved: $reservedQuantity, Requested: $quantity",
            )
        }

        availableQuantity += quantity
        reservedQuantity -= quantity
        updatedAt = Instant.now()
    }

    fun deductReserved(quantity: Int) {
        if (reservedQuantity < quantity) {
            throw InvalidOperationException(
                "Cannot deduct more than reserved quantity for product $productId. Reserved: $reservedQuantity, Requested: $quantity",
            )
        }

        reservedQuantity -= quantity
        updatedAt = Instant.now()
    }

    val totalQuantity: Int
        get() = availableQuantity + reservedQuantity

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryItem) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

@Entity
@Table(name = "processed_events")
class ProcessedEvent(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedEvent) return false
        return eventId == other.eventId
    }

    override fun hashCode(): Int = eventId.hashCode()
}

class InsufficientInventoryException(
    message: String,
) : RuntimeException(message)

class InvalidOperationException(
    message: String,
) : RuntimeException(message)
