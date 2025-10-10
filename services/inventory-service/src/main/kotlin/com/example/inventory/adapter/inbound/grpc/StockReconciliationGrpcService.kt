package com.example.inventory.adapter.inbound.grpc

import com.example.inventory.application.ReconcileInventoryCommand
import com.example.inventory.application.port.input.StockReconciliationUseCase
import com.example.inventory.proto.AdjustmentStatus
import com.example.inventory.proto.ReconcileStockRequest
import com.example.inventory.proto.ReconcileStockResponse
import com.example.inventory.proto.StockReconciliationServiceGrpc
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StockReconciliationGrpcService(
    private val stockReconciliation: StockReconciliationUseCase,
) : StockReconciliationServiceGrpc.StockReconciliationServiceImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun reconcileStock(
        request: ReconcileStockRequest,
        responseObserver: StreamObserver<ReconcileStockResponse>,
    ) {
        if (request.adjustmentsCount == 0) {
            logger.info("Received empty reconciliation request {}", request.requestId)
            responseObserver.onNext(ReconcileStockResponse.getDefaultInstance())
            responseObserver.onCompleted()
            return
        }

        val statuses =
            request.adjustmentsList.map { adjustment ->
                val sku = adjustment.sku.trim()
                when {
                    sku.isEmpty() ->
                        AdjustmentStatus
                            .newBuilder()
                            .setSku("")
                            .setApplied(false)
                            .setMessage("sku must be provided")
                            .build()

                    adjustment.availableQuantity < 0 ->
                        AdjustmentStatus
                            .newBuilder()
                            .setSku(sku)
                            .setApplied(false)
                            .setMessage("availableQuantity must be >= 0")
                            .build()

                    else ->
                        processAdjustment(
                            sku = sku,
                            availableQuantity = adjustment.availableQuantity,
                        )
                }
            }

        val response =
            ReconcileStockResponse
                .newBuilder()
                .addAllStatuses(statuses)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun processAdjustment(
        sku: String,
        availableQuantity: Int,
    ): AdjustmentStatus =
        try {
            stockReconciliation.reconcile(
                ReconcileInventoryCommand(
                    sku = sku,
                    availableQuantity = availableQuantity,
                ),
            )
            AdjustmentStatus
                .newBuilder()
                .setSku(sku)
                .setApplied(true)
                .setMessage("applied")
                .build()
        } catch (ex: Exception) {
            logger.warn("Failed to reconcile stock for sku {}", sku, ex)
            AdjustmentStatus
                .newBuilder()
                .setSku(sku)
                .setApplied(false)
                .setMessage((ex.message ?: "reconciliation failed").take(MAX_MESSAGE_LENGTH))
                .build()
        }

    private companion object {
        private const val MAX_MESSAGE_LENGTH = 256
    }
}
