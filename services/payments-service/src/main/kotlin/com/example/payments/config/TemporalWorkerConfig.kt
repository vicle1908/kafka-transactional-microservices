package com.example.payments.config

import com.example.payments.activity.PaymentActivityImpl
import com.example.payments.activity.RefundPaymentActivityImpl
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
 * Configuration for Temporal worker in the payments service.
 * Sets up activity workers to handle payment-related tasks from Temporal workflows.
 */
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = false)
class TemporalWorkerConfig {

    private val logger = LoggerFactory.getLogger(TemporalWorkerConfig::class.java)

    @Bean(destroyMethod = "shutdown")
    fun workerFactory(
        workflowServiceStubs: WorkflowServiceStubs,
        workflowClient: WorkflowClient,
        paymentActivityImpl: PaymentActivityImpl,
        refundPaymentActivityImpl: RefundPaymentActivityImpl,
        meterRegistry: MeterRegistry
    ): WorkerFactory {
        logger.info("Creating Temporal worker factory for payments service")

        val factory = WorkerFactory.newInstance(workflowServiceStubs, workflowClient.options.toBuilder()
            .setNamespace(workflowClient.options.namespace)
            .build())

        // Create worker for payments task queue
        val paymentsWorker: Worker = factory.newWorker(TaskQueues.PAYMENTS_TASK_QUEUE)

        // Register payment activities
        paymentsWorker.registerActivitiesImplementations(paymentActivityImpl, refundPaymentActivityImpl)

        // Add metrics for worker monitoring
        val workerStartCounter = meterRegistry.counter("temporal.worker.started", "service", "payments")
        val activitiesRegisteredGauge = meterRegistry.gauge("temporal.worker.activities.registered", 2)

        workerStartCounter.increment()
        logger.info("Started Temporal worker for payments task queue with ${activitiesRegisteredGauge.toInt()} activities")

        // Start the factory
        factory.start()

        return factory
    }
}