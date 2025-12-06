package com.example.temporal.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class TemporalObservabilityConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(TemporalObservabilityConfig::class.java)

    @Test
    fun `should create WorkerFactoryOptions with tracing when enabled`() {
        val spanExporter = InMemorySpanExporter.create()
        val openTelemetry =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    SdkTracerProvider
                        .builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                        .build(),
                ).build()

        contextRunner
            .withBean(OpenTelemetry::class.java, { openTelemetry })
            .withPropertyValues("temporal.tracing.enabled=true")
            .run { context: org.springframework.context.ConfigurableApplicationContext ->
                val workerFactoryOptions = context.getBean(io.temporal.worker.WorkerFactoryOptions::class.java)
                assertThat(workerFactoryOptions).isNotNull
                assertThat(workerFactoryOptions.workerInterceptors).isNotEmpty
            }
    }

    @Test
    fun `should create WorkerFactoryOptions without tracing when disabled`() {
        contextRunner
            .withPropertyValues("temporal.tracing.enabled=false")
            .run { context: org.springframework.context.ConfigurableApplicationContext ->
                val workerFactoryOptions = context.getBean(io.temporal.worker.WorkerFactoryOptions::class.java)
                assertThat(workerFactoryOptions).isNotNull
            }
    }

    @Test
    fun `should create WorkflowServiceStubsOptions`() {
        contextRunner.run { context: org.springframework.context.ConfigurableApplicationContext ->
            val stubsOptions = context.getBean(io.temporal.serviceclient.WorkflowServiceStubsOptions::class.java)
            assertThat(stubsOptions).isNotNull
        }
    }
}
