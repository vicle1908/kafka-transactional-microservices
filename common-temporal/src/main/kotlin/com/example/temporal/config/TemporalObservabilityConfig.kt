@file:Suppress("SpreadOperator")

package com.example.temporal.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.opentracingshim.OpenTracingShim
import io.temporal.client.WorkflowClientOptions
import io.temporal.opentracing.OpenTracingClientInterceptor
import io.temporal.opentracing.OpenTracingOptions
import io.temporal.opentracing.OpenTracingWorkerInterceptor
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.spring.boot.TemporalOptionsCustomizer
import io.temporal.worker.WorkerFactoryOptions
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(TemporalTracingProperties::class)
class TemporalObservabilityConfig {
    @Bean
    fun workflowServiceStubsOptions(): WorkflowServiceStubsOptions = WorkflowServiceStubsOptions.newBuilder().build()

    @Bean
    fun workerFactoryOptions(
        tracingProperties: TemporalTracingProperties,
        openTelemetryProvider: ObjectProvider<OpenTelemetry>,
    ): WorkerFactoryOptions {
        val builder = WorkerFactoryOptions.newBuilder()
        createOpenTracingOptions(tracingProperties, openTelemetryProvider.ifAvailable)?.let {
            builder.setWorkerInterceptors(OpenTracingWorkerInterceptor(it))
        }
        return builder.build()
    }

    @Suppress("SpreadOperator")
    @Bean
    fun workflowClientTracingCustomizer(
        tracingProperties: TemporalTracingProperties,
        openTelemetryProvider: ObjectProvider<OpenTelemetry>,
    ): TemporalOptionsCustomizer<WorkflowClientOptions.Builder>? {
        val tracingOptions =
            createOpenTracingOptions(tracingProperties, openTelemetryProvider.ifAvailable) ?: return null
        val interceptor = OpenTracingClientInterceptor(tracingOptions)
        return TemporalOptionsCustomizer { builder ->
            val existing = builder.build().interceptors ?: emptyArray()
            builder.setInterceptors(*(existing + interceptor))
        }
    }

    private fun createOpenTracingOptions(
        tracingProperties: TemporalTracingProperties,
        openTelemetry: OpenTelemetry?,
    ): OpenTracingOptions? {
        if (!tracingProperties.enabled || openTelemetry == null) {
            return null
        }
        val tracer = OpenTracingShim.createTracerShim(openTelemetry)
        return OpenTracingOptions.newBuilder().setTracer(tracer).build()
    }
}
