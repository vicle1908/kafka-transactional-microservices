package com.example.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InventoryReservationRepository : JpaRepository<InventoryReservationEntity, UUID> {
    fun findAllByOrderId(orderId: UUID): List<InventoryReservationEntity>
}
