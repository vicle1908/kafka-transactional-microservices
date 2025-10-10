package com.example.observability.config

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MicrometerConfig {
    @Bean
    @Primary
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect = ObservedAspect(observationRegistry)

    @Bean
    fun customMetricsBinder(): MeterBinder = MeterBinder { }
}
