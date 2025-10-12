package com.example.notifications.config

import com.example.notifications.activity.NotificationActivityImpl
import com.example.temporal.TaskQueues
import io.micrometer.core.instrument.MeterRegistry
import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Temporal worker in the notifications service.
 * Sets up activity workers to handle notification-related tasks from Temporal workflows.
 */
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = false)
class TemporalWorkerConfig {

    private val logger = LoggerFactory.getLogger(TemporalWorkerConfig::class.java)

    @Bean(destroyMethod = "shutdown")
    fun workerFactory(
        workflowServiceStubs: WorkflowServiceStubs,
        workflowClient: WorkflowClient,
        notificationActivityImpl: NotificationActivityImpl,
        meterRegistry: MeterRegistry
    ): WorkerFactory {
        logger.info("Creating Temporal worker factory for notifications service")

        val factory = WorkerFactory.newInstance(workflowServiceStubs, workflowClient.options.toBuilder()
            .setNamespace(workflowClient.options.namespace)
            .build())

        // Create worker for notifications task queue
        val notificationsWorker: Worker = factory.newWorker(TaskQueues.NOTIFICATIONS_TASK_QUEUE)

        // Register notification activities
        notificationsWorker.registerActivitiesImplementations(notificationActivityImpl)

        // Add metrics for worker monitoring
        val workerStartCounter = meterRegistry.counter("temporal.worker.started", "service", "notifications")
        val activitiesRegisteredGauge = meterRegistry.gauge("temporal.worker.activities.registered", 1)

        workerStartCounter.increment()
        logger.info("Started Temporal worker for notifications task queue with ${activitiesRegisteredGauge.toInt()} activities")

        // Start the factory
        factory.start()

        return factory
    }
}