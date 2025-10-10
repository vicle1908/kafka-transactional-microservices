package com.example.notification

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, controlledShutdown = true, topics = ["notification.test"])
class NotificationServiceApplicationTests : NotificationServiceIntegrationTestSupport() {
    @Test
    fun contextLoads() {
        // Placeholder; real notification tests will validate downstream integrations.
    }
}
