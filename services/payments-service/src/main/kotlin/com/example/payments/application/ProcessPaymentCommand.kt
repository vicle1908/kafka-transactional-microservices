package com.example.payments.application

import java.math.BigDecimal
import java.util.UUID

data class ProcessPaymentCommand(
    val orderId: UUID,
    val amount: BigDecimal,
    val eventId: UUID? = null,
    val metadata: Map<String, String> = emptyMap(),
)
