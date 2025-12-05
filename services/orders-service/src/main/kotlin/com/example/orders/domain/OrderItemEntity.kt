package com.example.orders.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Suppress("ProtectedInFinal")
@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    val order: OrderEntity,
    @Column(name = "product_id", nullable = false)
    val productId: String,
    @Column(name = "product_name", nullable = false)
    val productName: String,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Column(name = "unit_price", nullable = false)
    val unitPrice: BigDecimal,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set
}
