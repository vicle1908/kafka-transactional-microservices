package com.example.orders.application

import java.math.BigDecimal

data class CreateOrderCommand(
    val customerId: String,
    val orderItems: List<OrderItemCommand>,
)

data class OrderItemCommand(
    val productId: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val productName: String? = null,
)
