package com.example.inventory.domain

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InventoryStockRepository : JpaRepository<InventoryStockEntity, UUID> {
    fun findBySku(sku: String): InventoryStockEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InventoryStockEntity s where s.sku = :sku")
    fun lockBySku(
        @Param("sku") sku: String,
    ): InventoryStockEntity?
}
