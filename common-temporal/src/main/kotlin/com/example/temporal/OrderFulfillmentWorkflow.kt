package com.example.temporal

import com.example.temporal.workflow.OrderFulfillmentResult
import com.example.temporal.workflow.WorkflowStatus
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.util.UUID

@WorkflowInterface
interface OrderFulfillmentWorkflow {
    @WorkflowMethod
    fun execute(orderId: UUID): OrderFulfillmentResult

    @QueryMethod
    fun getStatus(): WorkflowStatus

    @SignalMethod
    fun cancel(reason: String)
}
