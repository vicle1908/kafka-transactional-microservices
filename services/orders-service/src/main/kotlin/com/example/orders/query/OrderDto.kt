package com.example.orders.query

import com.example.orders.domain.OrderEntity
import com.example.orders.domain.OrderItemEntity
import com.example.orders.domain.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderDto(
    val id: UUID,
    val customerId: String,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val createdAt: Instant,
    val orderItems: List<OrderItemDto> = emptyList(),
)

data class OrderItemDto(
    val id: UUID,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val createdAt: Instant,
)

internal fun OrderEntity.toDto(orderItems: List<OrderItemEntity>): OrderDto? =
    this.id?.let {
        OrderDto(
            id = it,
            customerId = this.customerId,
            status = this.status,
            totalAmount = this.totalAmount,
            createdAt = this.createdAt,
            orderItems =
                orderItems.map { item ->
                    OrderItemDto(
                        id = item.id!!,
                        productId = item.productId,
                        productName = item.productName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        createdAt = item.createdAt,
                    )
                },
        )
    }
