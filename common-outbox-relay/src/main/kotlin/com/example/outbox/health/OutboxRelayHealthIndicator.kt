package com.example.outbox.health

import com.example.outbox.entity.OutboxStatus
import com.example.outbox.repository.OutboxRepository
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class OutboxRelayHealthIndicator(
    private val outboxRepository: OutboxRepository,
) : HealthIndicator {
    companion object {
        private const val HIGH_PENDING_THRESHOLD = 1000L
        private const val CRITICAL_PENDING_THRESHOLD = 5000L
    }

    override fun health(): Health {
        val pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING)
        val failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED)

        return when {
            pendingCount > CRITICAL_PENDING_THRESHOLD -> {
                Health
                    .down()
                    .withDetail("pendingMessages", pendingCount)
                    .withDetail("failedMessages", failedCount)
                    .withDetail("status", "CRITICAL")
                    .withDetail("message", "Too many pending messages in outbox")
                    .build()
            }
            pendingCount > HIGH_PENDING_THRESHOLD -> {
                Health
                    .down()
                    .withDetail("pendingMessages", pendingCount)
                    .withDetail("failedMessages", failedCount)
                    .withDetail("status", "WARNING")
                    .withDetail("message", "High number of pending messages in outbox")
                    .build()
            }
            else -> {
                Health
                    .up()
                    .withDetail("pendingMessages", pendingCount)
                    .withDetail("failedMessages", failedCount)
                    .withDetail("status", "OK")
                    .build()
            }
        }
    }
}
