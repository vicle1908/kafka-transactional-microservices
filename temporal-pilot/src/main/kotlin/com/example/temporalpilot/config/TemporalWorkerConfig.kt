package com.example.temporalpilot.config

import com.example.temporal.TaskQueues
import com.example.temporalpilot.implementation.OrderFulfillmentWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import io.temporal.worker.WorkerFactoryOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Temporal worker configuration for the temporal-pilot service.
 *
 * This configuration uses the auto-configured WorkflowClient from temporal-spring-boot-starter
 * and creates a WorkerFactory to register workflow implementations.
 */
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = true)
class TemporalWorkerConfig {
    private val logger = LoggerFactory.getLogger(TemporalWorkerConfig::class.java)

    @Bean(destroyMethod = "shutdown")
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory {
        logger.info("Creating Temporal worker factory for OrderFulfillmentWorkflow")

        val options = WorkerFactoryOptions.newBuilder().build()
        val factory = WorkerFactory.newInstance(workflowClient, options)

        // Create worker for OrderFulfillmentWorkflow task queue
        val worker: Worker = factory.newWorker(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
        worker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl::class.java)

        logger.info(
            "Registered OrderFulfillmentWorkflowImpl on task queue: {}",
            TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE,
        )

        factory.start()
        logger.info("Temporal worker factory started successfully")

        return factory
    }
}
