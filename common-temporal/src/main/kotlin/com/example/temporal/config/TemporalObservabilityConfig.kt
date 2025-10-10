package com.example.temporal.config

import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactoryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalObservabilityConfig {

    @Bean
    fun workflowServiceStubsOptions(): WorkflowServiceStubsOptions =
        WorkflowServiceStubsOptions.newBuilder().build()

    @Bean
    fun workerFactoryOptions(): WorkerFactoryOptions =
        WorkerFactoryOptions.newBuilder().build()
}
