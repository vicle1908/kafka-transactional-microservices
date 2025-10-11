package com.example.orders.query

import com.example.orders.domain.OrderEntity
import com.example.orders.domain.OrderStatus
import java.time.Instant
import java.util.UUID

data class OrderDto(
    val id: UUID,
    val customerId: String,
    val status: OrderStatus,
    val createdAt: Instant,
)

internal fun OrderEntity.toDto(): OrderDto? =
    this.id?.let { OrderDto(id = it, customerId = this.customerId, status = this.status, createdAt = this.createdAt) }
