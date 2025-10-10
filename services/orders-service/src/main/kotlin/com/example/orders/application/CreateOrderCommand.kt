package com.example.orders.application

data class CreateOrderCommand(
    val customerId: String,
    val orderItems: List<String>,
)
