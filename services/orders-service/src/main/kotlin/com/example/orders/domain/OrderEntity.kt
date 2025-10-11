package com.example.orders.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Suppress("ProtectedInFinal")
@Entity
@Table(name = "orders")
class OrderEntity(
    @Column(name = "customer_id", nullable = false)
    val customerId: String,
    @Column(name = "status", nullable = false)
    val status: OrderStatus,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set
}
