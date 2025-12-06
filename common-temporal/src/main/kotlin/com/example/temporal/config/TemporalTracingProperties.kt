package com.example.temporal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "temporal.tracing")
data class TemporalTracingProperties(
    val enabled: Boolean = true,
)
