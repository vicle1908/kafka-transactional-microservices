package com.example.observability.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryConfig {
    @Value("\${otel.service.name:my-kafka-microservice}")
    private lateinit var serviceName: String

    @Value("\${otel.endpoint:localhost:4317}")
    private lateinit var endpoint: String

    @Value("\${otel.enabled:true}")
    private var enabled: Boolean = true

    @Bean
    fun openTelemetry(): OpenTelemetry {
        if (!enabled) {
            return OpenTelemetry.noop()
        }

        val resource =
            Resource
                .getDefault()
                .toBuilder()
                .put("service.name", serviceName)
                .build()

        val spanExporter =
            OtlpGrpcSpanExporter
                .builder()
                .setEndpoint("http://$endpoint")
                .build()

        val spanProcessor = BatchSpanProcessor.builder(spanExporter).build()

        val tracerProvider =
            SdkTracerProvider
                .builder()
                .addSpanProcessor(spanProcessor)
                .setResource(resource)
                .build()

        return OpenTelemetrySdk
            .builder()
            .setTracerProvider(tracerProvider)
            .build()
    }

    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer = openTelemetry.getTracer(serviceName)
}
