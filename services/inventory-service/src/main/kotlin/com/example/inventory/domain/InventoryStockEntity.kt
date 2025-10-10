@file:Suppress("JpaDataSourceORMInspection", "unused")

package com.example.inventory.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "inventory_stock")
class InventoryStockEntity(
    @Column(name = "sku", nullable = false, unique = true)
    val sku: String,
    @Column(name = "available_quantity", nullable = false)
    private var availableQuantity: Int,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        private set

    @Version
    private var version: Long? = null

    fun availableQuantity(): Int = availableQuantity

    fun reserve(quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        if (availableQuantity < quantity) {
            throw IllegalStateException("Insufficient stock for sku=$sku")
        }
        availableQuantity -= quantity
    }

    fun release(quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        availableQuantity += quantity
    }

    fun reconcile(quantity: Int) {
        require(quantity >= 0) { "quantity must be non-negative" }
        availableQuantity = quantity
    }

    constructor() : this("", 0)
}
