package com.example.payments.implementation

import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.RefundPaymentActivity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentActivityImpl :
    PaymentActivity,
    RefundPaymentActivity {
    override fun processPayment(orderId: UUID) {
        println("*** Processing payment for order: $orderId in payments-service ***")
        // Real payment logic will go here
    }

    override fun refundPayment(orderId: UUID) {
        println("*** Refunding payment for order: $orderId in payments-service ***")
        // Real refund logic will go here
    }
}
