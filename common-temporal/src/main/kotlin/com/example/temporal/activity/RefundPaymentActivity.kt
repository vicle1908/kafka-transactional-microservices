package com.example.temporal.activity

import io.temporal.activity.ActivityInterface
import java.util.UUID

@ActivityInterface
interface RefundPaymentActivity {
    fun refundPayment(orderId: UUID)
}
