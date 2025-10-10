package com.example.outbox.config

import com.example.outbox.processor.ScheduledOutboxProcessor
import com.example.outbox.service.OutboxRelayService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = ["outbox.relay.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class OutboxRelayConfiguration
