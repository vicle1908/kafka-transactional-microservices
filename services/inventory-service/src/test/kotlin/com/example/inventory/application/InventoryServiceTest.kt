package com.example.inventory.application

import com.example.inventory.InventoryServiceApplication
import com.example.inventory.domain.InventoryReservationRepository
import com.example.inventory.domain.InventoryStockEntity
import com.example.inventory.domain.InventoryStockRepository
import com.example.inventory.testsupport.InventoryContainers
import com.example.inventory.testsupport.InventoryFlywayTestConfig
import com.example.outbox.repository.OutboxRepository
import com.example.saga.SagaNames
import com.example.saga.SagaStateRepository
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.saga.SagaTransitionOptions
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    classes = [InventoryServiceApplication::class],
    properties = ["spring.kafka.listener.auto-startup=false"],
)
@ActiveProfiles("test")
@Import(InventoryFlywayTestConfig::class, com.example.inventory.testsupport.TestCacheConfig::class)
class InventoryServiceTest {
    @Autowired
    private lateinit var inventoryService: InventoryService

    @Autowired
    private lateinit var reservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var stockRepository: InventoryStockRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    @Autowired
    private lateinit var sagaStateRepository: SagaStateRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private val json = Json { ignoreUnknownKeys = false }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerDataSource(registry: DynamicPropertyRegistry) {
            InventoryContainers.registerPostgres(registry)
        }
    }

    @BeforeEach
    fun cleanRepositories() {
        reservationRepository.deleteAll()
        outboxRepository.deleteAll()
        sagaStateRepository.deleteAll()
        stockRepository.deleteAll()
    }

    @Test
    fun `reserve should persist reservation and append outbox event`() {
        val counterBefore = metricCount(SagaStepNames.INVENTORY_RESERVED, SagaStatus.IN_PROGRESS)

        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        advanceSagaToInProgress(orderId, creationInstant)

        val command =
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "sku-123",
                quantity = 2,
            )

        stockRepository.save(
            InventoryStockEntity(
                sku = command.sku,
                availableQuantity = 10,
            ),
        )

        val reservationId = inventoryService.reserve(command)

        val reservations = reservationRepository.findAll()
        val events = outboxRepository.findAll()

        assertThat(reservationId).isNotNull
        assertThat(reservations).hasSize(1)
        assertThat(events).hasSize(1)

        val storedEvent = events.first()
        val envelope = json.parseToJsonElement(storedEvent.payload).jsonObject
        val payload = json.parseToJsonElement(envelope["payload"]!!.jsonPrimitive.content).jsonObject

        assertThat(envelope["aggregate_id"]!!.jsonPrimitive.content).isEqualTo(reservationId.toString())
        assertThat(payload["reservationId"]!!.jsonPrimitive.content).isEqualTo(reservationId.toString())
        assertThat(payload["orderId"]!!.jsonPrimitive.content).isEqualTo(command.orderId.toString())
        assertThat(payload["sku"]!!.jsonPrimitive.content).isEqualTo(command.sku)
        assertThat(payload["quantity"]!!.jsonPrimitive.int).isEqualTo(2)
        assertThat(payload["status"]!!.jsonPrimitive.content).isEqualTo("RESERVED")

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)
        assertThat(saga.data()).contains(SagaStepNames.INVENTORY_RESERVED)

        val counterAfter = metricCount(SagaStepNames.INVENTORY_RESERVED, SagaStatus.IN_PROGRESS)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
    }

    @Test
    fun `reserve should reject invalid input`() {
        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        advanceSagaToInProgress(orderId, creationInstant)

        val command =
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "   ",
                quantity = 0,
            )

        stockRepository.save(
            InventoryStockEntity(
                sku = "sku-123",
                availableQuantity = 10,
            ),
        )

        assertThatThrownBy { inventoryService.reserve(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("sku")

        assertThat(reservationRepository.count()).isZero()
        assertThat(outboxRepository.count()).isZero()
        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.IN_PROGRESS)
    }

    @Test
    fun `release should mark saga failed with inventory released step`() {
        val counterBefore = metricCount(SagaStepNames.INVENTORY_RELEASED, SagaStatus.FAILED)

        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        advanceSagaToInProgress(orderId, creationInstant)
        stockRepository.save(
            InventoryStockEntity(
                sku = "sku-321",
                availableQuantity = 5,
            ),
        )
        inventoryService.reserve(
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "sku-321",
                quantity = 2,
            ),
        )
        assertThat(stockRepository.findBySku("sku-321")!!.availableQuantity()).isEqualTo(3)
        inventoryService.release(orderId, "stock shortfall")

        val saga =
            sagaStateRepository.findBySagaTypeAndCorrelationId(
                SagaNames.ORDER_FULFILLMENT,
                orderId.toString(),
            )
        assertThat(saga).isNotNull
        assertThat(saga!!.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.data()).contains(SagaStepNames.INVENTORY_RELEASED)
        assertThat(saga.data()).contains("stock shortfall")

        val counterAfter = metricCount(SagaStepNames.INVENTORY_RELEASED, SagaStatus.FAILED)
        assertThat(counterAfter - counterBefore).isEqualTo(1.0)
        assertThat(stockRepository.findBySku("sku-321")!!.availableQuantity()).isEqualTo(5)
    }

    @Test
    fun `reserve should fail when stock is insufficient`() {
        val orderId = UUID.randomUUID()
        val creationInstant = Instant.parse("2025-01-01T00:00:00Z")
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, creationInstant),
            at = creationInstant,
        )
        advanceSagaToInProgress(orderId, creationInstant)

        stockRepository.save(
            InventoryStockEntity(
                sku = "sku-555",
                availableQuantity = 1,
            ),
        )

        val command =
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "sku-555",
                quantity = 5,
            )

        assertThatThrownBy { inventoryService.reserve(command) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Insufficient stock")
        assertThat(reservationRepository.count()).isZero()
    }

    private fun metricCount(
        step: String,
        state: SagaStatus,
    ): Double {
        val counter =
            meterRegistry.counter(
                "saga.step.processed",
                listOf(
                    Tag.of("sagaType", SagaNames.ORDER_FULFILLMENT),
                    Tag.of("step", step),
                    Tag.of("state", state.name),
                ),
            )
        return counter.count()
    }

    private fun advanceSagaToInProgress(
        orderId: UUID,
        creationInstant: Instant,
    ) {
        val options = paymentCompletedOptions(creationInstant)
        sagaStateService.transitionByCorrelation(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            newState = SagaStatus.IN_PROGRESS,
            options = options,
        )
    }

    private fun paymentCompletedOptions(creationInstant: Instant): SagaTransitionOptions =
        SagaTransitionOptions(
            expectedState = SagaStatus.STARTED,
            dataTransformer = { existing ->
                SagaStepFormatter.append(existing, SagaStepNames.PAYMENT_COMPLETED, creationInstant)
            },
            occurredAt = creationInstant,
        )
}
