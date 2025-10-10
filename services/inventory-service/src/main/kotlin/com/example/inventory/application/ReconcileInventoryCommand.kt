package com.example.inventory.application

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ReconcileInventoryCommand(
    @field:NotBlank
    val sku: String,
    @field:Min(0)
    val availableQuantity: Int,
)
