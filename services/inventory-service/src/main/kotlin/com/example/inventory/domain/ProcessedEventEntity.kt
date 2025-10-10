@file:Suppress("JpaDataSourceORMInspection", "unused")

package com.example.inventory.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "processed_events")
class ProcessedEventEntity(
    @Id
    @Column(name = "event_id", nullable = false)
    val eventId: UUID,
    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant,
) {
    constructor() : this(UUID.randomUUID(), Instant.EPOCH)
}
