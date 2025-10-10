package com.example.payments.application.port.out

import java.math.BigDecimal
import java.util.UUID

data class RefundRequest(
    val refundId: UUID,
    val paymentId: UUID,
    val orderId: UUID,
    val amount: BigDecimal,
    val reason: String?,
)

sealed interface RefundResult {
    data object Completed : RefundResult

    data class Failed(
        val reason: String,
    ) : RefundResult
}

fun interface RefundGateway {
    fun refund(request: RefundRequest): RefundResult
}
