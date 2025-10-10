package com.example.inventory.application.port.input

import com.example.inventory.application.ReconcileInventoryCommand

fun interface StockReconciliationUseCase {
    fun reconcile(command: ReconcileInventoryCommand)
}
