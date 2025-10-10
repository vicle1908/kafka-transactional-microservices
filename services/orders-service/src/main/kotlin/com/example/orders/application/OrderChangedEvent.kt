package com.example.orders.application

import java.util.UUID

data class OrderChangedEvent(
    val orderId: UUID,
)
