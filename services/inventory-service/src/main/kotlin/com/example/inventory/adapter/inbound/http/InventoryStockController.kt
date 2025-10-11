package com.example.inventory.adapter.inbound.http

import com.example.inventory.application.ReconcileInventoryCommand
import com.example.inventory.application.port.input.StockReconciliationUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventory/stock")
class InventoryStockController(
    private val stockReconciliation: StockReconciliationUseCase,
) {
    @PutMapping("/{sku}")
    fun reconcile(
        @PathVariable sku: String,
        @Valid @RequestBody request: ReconcileInventoryRequest,
    ): ResponseEntity<Void> {
        stockReconciliation.reconcile(
            ReconcileInventoryCommand(
                sku = sku,
                availableQuantity = request.availableQuantity,
            ),
        )
        return ResponseEntity.noContent().build()
    }
}

data class ReconcileInventoryRequest(
    @field:Min(0)
    val availableQuantity: Int,
)
