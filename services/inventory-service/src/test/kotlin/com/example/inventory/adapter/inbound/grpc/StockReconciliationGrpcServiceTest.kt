package com.example.inventory.adapter.inbound.grpc

import com.example.inventory.application.ReconcileInventoryCommand
import com.example.inventory.application.port.input.StockReconciliationUseCase
import com.example.inventory.proto.ReconcileStockRequest
import com.example.inventory.proto.StockAdjustment
import com.example.inventory.proto.StockReconciliationServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class StockReconciliationGrpcServiceTest {
    private lateinit var reconciliationStub: RecordingReconciliationUseCase
    private lateinit var grpcService: StockReconciliationGrpcService
    private lateinit var blockingClient: StockReconciliationServiceGrpc.StockReconciliationServiceBlockingStub
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel

    @BeforeEach
    fun setUp() {
        reconciliationStub = RecordingReconciliationUseCase()
        grpcService = StockReconciliationGrpcService(reconciliationStub)
        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(grpcService)
                .build()
                .start()
        channel =
            InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()
        blockingClient = StockReconciliationServiceGrpc.newBlockingStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        channel.awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow()
        server.awaitTermination(5, TimeUnit.SECONDS)
        reconciliationStub.reset()
    }

    @Test
    fun `reconcile success applies each adjustment`() {
        val response =
            blockingClient.reconcileStock(
                ReconcileStockRequest
                    .newBuilder()
                    .setRequestId("req-1")
                    .addAdjustments(newAdjustment("SKU-1", 5))
                    .addAdjustments(newAdjustment("SKU-2", 10))
                    .build(),
            )

        assertThat(response.statusesList).hasSize(2)
        assertThat(response.statusesList.map { it.applied }).containsExactly(true, true)
        assertThat(reconciliationStub.commands.map { it.sku }).containsExactly("SKU-1", "SKU-2")
    }

    @Test
    fun `reconcile returns failure status when sku missing`() {
        val response =
            blockingClient.reconcileStock(
                ReconcileStockRequest
                    .newBuilder()
                    .setRequestId("req-2")
                    .addAdjustments(newAdjustment("", 5))
                    .build(),
            )

        assertThat(response.statusesList).hasSize(1)
        val status = response.statusesList.first()
        assertThat(status.applied).isFalse()
        assertThat(status.message).contains("sku")
        assertThat(reconciliationStub.commands).isEmpty()
    }

    @Test
    fun `reconcile reports failure message when reconciliation throws`() {
        reconciliationStub.failForSku = "SKU-FAIL"

        val response =
            blockingClient.reconcileStock(
                ReconcileStockRequest
                    .newBuilder()
                    .setRequestId("req-3")
                    .addAdjustments(newAdjustment("SKU-FAIL", 3))
                    .build(),
            )

        assertThat(response.statusesList).hasSize(1)
        val status = response.statusesList.first()
        assertThat(status.applied).isFalse()
        assertThat(status.message).contains("simulated")
    }

    private fun newAdjustment(
        sku: String,
        availableQuantity: Int,
    ): StockAdjustment =
        StockAdjustment
            .newBuilder()
            .setSku(sku)
            .setAvailableQuantity(availableQuantity)
            .build()

    private class RecordingReconciliationUseCase : StockReconciliationUseCase {
        val commands = mutableListOf<ReconcileInventoryCommand>()
        var failForSku: String? = null

        override fun reconcile(command: ReconcileInventoryCommand) {
            if (command.sku == failForSku) {
                throw IllegalStateException("simulated failure")
            }
            commands += command
        }

        fun reset() {
            commands.clear()
            failForSku = null
        }
    }
}
