package com.example.events

import java.time.Instant
import java.util.UUID

data class OrderCreatedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val customerId: String,
    val occurredAt: Instant,
    val payload: String,
    val headers: Map<String, String> = emptyMap(),
)
