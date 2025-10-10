package com.example.temporalpilot

import com.example.temporal.OrderFulfillmentWorkflow
import com.example.temporal.TaskQueues
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.util.UUID

@SpringBootApplication
class TemporalPilotApplication

fun main(args: Array<String>) {
    runApplication<TemporalPilotApplication>(*args)
}

@Component
class WorkflowStarter(
    private val workflowClient: WorkflowClient,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val workflowOptions =
            WorkflowOptions
                .newBuilder()
                .setTaskQueue(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
                .build()
        val workflow = workflowClient.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowOptions)
        WorkflowClient.start(workflow::start, UUID.randomUUID())
    }
}
