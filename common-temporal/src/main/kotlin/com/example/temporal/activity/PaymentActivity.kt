package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.UUID

@ActivityInterface
interface PaymentActivity {
    fun getOrderAmount(orderId: UUID): java.math.BigDecimal?

    fun processPayment(
        orderId: UUID,
        amount: java.math.BigDecimal,
    ): PaymentResult
}
