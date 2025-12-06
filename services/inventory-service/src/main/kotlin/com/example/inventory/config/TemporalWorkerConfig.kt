package com.example.inventory.config

import com.example.inventory.activity.InventoryActivityImpl
import com.example.temporal.TaskQueues
import io.micrometer.core.instrument.MeterRegistry
import io.temporal.client.WorkflowClient
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import io.temporal.worker.WorkerFactoryOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Temporal worker in the inventory service.
 * Sets up activity workers to handle inventory-related tasks from Temporal workflows.
 */
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = false)
class TemporalWorkerConfig {
    private val logger = LoggerFactory.getLogger(TemporalWorkerConfig::class.java)

    @Bean(destroyMethod = "shutdown")
    fun workerFactory(
        workflowClient: WorkflowClient,
        workerFactoryOptions: WorkerFactoryOptions,
        inventoryActivityImpl: InventoryActivityImpl,
        meterRegistry: MeterRegistry,
    ): WorkerFactory {
        logger.info("Creating Temporal worker factory for inventory service")

        val factory =
            WorkerFactory.newInstance(
                workflowClient,
                workerFactoryOptions,
            )

        // Create worker for inventory task queue
        val inventoryWorker: Worker = factory.newWorker(TaskQueues.INVENTORY_TASK_QUEUE)

        // Register inventory activities
        inventoryWorker.registerActivitiesImplementations(inventoryActivityImpl)

        // Add metrics for worker monitoring
        val workerStartCounter = meterRegistry.counter("temporal.worker.started", "service", "inventory")
        val activitiesRegisteredGauge =
            meterRegistry.gauge("temporal.worker.activities.registered", 1) ?: 0.0

        workerStartCounter.increment()
        logger.info(
            "Started Temporal worker for inventory task queue with ${activitiesRegisteredGauge.toInt()} activities",
        )

        // Start the factory
        factory.start()

        return factory
    }
}
