package com.example.temporalpilot.config

import io.temporal.worker.WorkerFactoryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalObservabilityConfig {
    @Bean
    fun workerFactoryOptions(): WorkerFactoryOptions =
        WorkerFactoryOptions
            .newBuilder()
            .build()
}
