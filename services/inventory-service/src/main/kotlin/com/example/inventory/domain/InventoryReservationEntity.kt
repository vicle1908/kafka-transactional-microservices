@file:Suppress("JpaDataSourceORMInspection", "unused")

package com.example.inventory.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_reservations")
class InventoryReservationEntity(
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(name = "sku", nullable = false)
    val sku: String,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: InventoryReservationStatus,
    @Column(name = "reserved_at", nullable = false)
    val reservedAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    constructor() : this(UUID.randomUUID(), "", 0, InventoryReservationStatus.PENDING, Instant.EPOCH, Instant.EPOCH)
}
