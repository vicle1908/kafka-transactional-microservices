package com.example.temporalpilot.config

import io.opentelemetry.api.OpenTelemetry
import io.temporal.common.interceptors.WorkerInterceptor
import io.temporal.worker.WorkerFactoryOptions
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalObservabilityConfig {
    @Bean
    fun workerFactoryOptions(openTelemetryProvider: ObjectProvider<OpenTelemetry>): WorkerFactoryOptions {
        val builder = WorkerFactoryOptions.newBuilder()
        val openTelemetry = openTelemetryProvider.ifAvailable

        if (openTelemetry != null) {
            // Try to attach OpenTelemetry interceptor if the temporal-opentelemetry artifact is available
            try {
                val clazz = Class.forName("io.temporal.opentelemetry.OpenTelemetryWorkerInterceptor")
                val ctor = clazz.getConstructor(OpenTelemetry::class.java)
                val interceptor = ctor.newInstance(openTelemetry)
                if (interceptor is WorkerInterceptor) {
                    builder.setWorkerInterceptors(interceptor)
                }
            } catch (_: Throwable) {
                // Interceptor not available; proceed without it
            }
        }
        return builder.build()
    }
}
