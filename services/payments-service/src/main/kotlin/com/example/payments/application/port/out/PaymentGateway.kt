package com.example.payments.application.port.out

import java.math.BigDecimal
import java.util.UUID

data class PaymentChargeRequest(
    val paymentId: UUID,
    val orderId: UUID,
    val amount: BigDecimal,
    val metadata: Map<String, String> = emptyMap(),
)

sealed interface PaymentChargeResult {
    data class Approved(
        val confirmationCode: String,
    ) : PaymentChargeResult

    data class Declined(
        val reason: String,
    ) : PaymentChargeResult
}

fun interface PaymentGateway {
    fun charge(request: PaymentChargeRequest): PaymentChargeResult
}
