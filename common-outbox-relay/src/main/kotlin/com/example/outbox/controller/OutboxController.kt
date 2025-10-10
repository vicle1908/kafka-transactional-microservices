package com.example.outbox.controller

import com.example.outbox.service.OutboxRelayService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/outbox")
class OutboxController(
    private val outboxRelayService: OutboxRelayService,
) {
    @PostMapping("/process")
    fun processPendingMessages(): ResponseEntity<Map<String, Any>> {
        val processedCount = outboxRelayService.processPendingMessages()
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to processedCount,
                "message" to "Processed $processedCount pending messages",
            ),
        )
    }

    @PostMapping("/replay/{messageId}")
    fun replayMessage(
        @PathVariable messageId: String,
    ): ResponseEntity<Map<String, Any>> {
        val success = outboxRelayService.replayMessage(messageId)
        return if (success) {
            ResponseEntity.ok(
                mapOf(
                    "message" to "Successfully replayed message $messageId",
                ),
            )
        } else {
            ResponseEntity.badRequest().body(
                mapOf(
                    "error" to "Failed to replay message $messageId",
                ),
            )
        }
    }

    @PostMapping("/replay-range")
    fun replayMessagesByTimeRange(
        @RequestParam startTime: Instant,
        @RequestParam endTime: Instant,
    ): ResponseEntity<Map<String, Any>> {
        val processedCount = outboxRelayService.processMessagesByTimeRange(startTime, endTime)
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to processedCount,
                "message" to "Processed $processedCount messages in time range",
            ),
        )
    }

    @GetMapping("/pending-count")
    fun getPendingMessageCount(): ResponseEntity<Map<String, Any>> {
        val count = outboxRelayService.getPendingMessageCount()
        return ResponseEntity.ok(
            mapOf(
                "pendingCount" to count,
                "message" to "Current pending outbox message count",
            ),
        )
    }
}
