package com.example.temporal

import com.example.temporal.workflow.OrderFulfillmentResult
import java.util.UUID

/**
 * Temporal workflow contract orchestrating the order fulfillment saga.
 * Implementations may expose both fire-and-forget (`start`) and synchronous (`execute`)
 * entry points to support different callers.
 */
interface OrderFulfillmentWorkflow {
    fun start(orderId: UUID)

    fun execute(orderId: UUID): OrderFulfillmentResult
}
