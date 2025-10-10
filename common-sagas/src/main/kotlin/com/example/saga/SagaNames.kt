package com.example.saga

object SagaNames {
    const val ORDER_FULFILLMENT = "order-fulfillment"
}

object SagaStepNames {
    const val ORDER_CREATED = "order-created"
    const val PAYMENT_COMPLETED = "payment-completed"
    const val PAYMENT_FAILED = "payment-failed"
    const val INVENTORY_RESERVED = "inventory-reserved"
    const val NOTIFICATION_SENT = "notification-sent"
    const val NOTIFICATION_FAILED = "notification-failed"
    const val PAYMENT_COMPENSATED = "payment-compensated"
    const val INVENTORY_RELEASED = "inventory-released"
}
