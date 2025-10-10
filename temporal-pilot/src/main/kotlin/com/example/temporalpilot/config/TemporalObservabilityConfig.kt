package com.example.temporalpilot.config

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.opentracingshim.OpenTracingShim
import io.opentracing.Tracer
import io.temporal.client.WorkflowClientOptions
import io.temporal.opentracing.OpenTracingClientInterceptor
import io.temporal.opentracing.OpenTracingOptions
import io.temporal.opentracing.OpenTracingWorkerInterceptor
import io.temporal.worker.WorkerFactoryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalObservabilityConfig {
    @Bean
    fun openTelemetry(): OpenTelemetry = GlobalOpenTelemetry.get()

    @Bean
    fun temporalTracer(openTelemetry: OpenTelemetry): Tracer = OpenTracingShim.createTracerShim(openTelemetry)

    @Bean
    fun temporalOpenTracingOptions(tracer: Tracer): OpenTracingOptions =
        OpenTracingOptions
            .newBuilder()
            .setTracer(tracer)
            .build()

    @Bean
    fun workerFactoryOptions(openTracingOptions: OpenTracingOptions): WorkerFactoryOptions =
        WorkerFactoryOptions
            .newBuilder()
            .setWorkerInterceptors(OpenTracingWorkerInterceptor(openTracingOptions))
            .build()

    @Bean
    fun workflowClientOptions(openTracingOptions: OpenTracingOptions): WorkflowClientOptions =
        WorkflowClientOptions
            .newBuilder()
            .setInterceptors(OpenTracingClientInterceptor(openTracingOptions))
            .build()
}
