package com.example.orders.adapter.inbound.http

import com.example.orders.application.OrderService
import com.example.temporal.workflow.WorkflowStatus
import com.example.temporal.workflow.WorkflowStep
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerWorkflowTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var orderService: OrderService

    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.0"))
                .withDatabaseName("orders_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)

        @Container
        @JvmStatic
        val kafka =
            KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
                .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.flyway.enabled") { false }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.kafka.consumer.group-id") { "orders-workflow-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
        }
    }

    @Test
    fun `should get workflow status`() {
        val orderId = UUID.randomUUID()
        val startedAt = Instant.now().minusSeconds(10)
        val completedAt = Instant.now()
        val workflowStatus =
            WorkflowStatus(
                orderId = orderId,
                status = "RUNNING",
                currentStep = "PAYMENT",
                steps =
                    listOf(
                        WorkflowStep(
                            stepName = "PAYMENT",
                            status = "COMPLETED",
                            startedAt = startedAt,
                            completedAt = completedAt,
                            duration =
                                java.time.Duration
                                    .between(startedAt, completedAt)
                                    .toMillis(),
                            result = "Payment processed",
                            error = null,
                        ),
                    ),
                startedAt = startedAt,
                cancelled = false,
                cancellationReason = null,
                progress = 33.3,
            )

        whenever(orderService.getWorkflowStatus(orderId)).thenReturn(workflowStatus)

        mockMvc
            .perform(get("/orders/$orderId/workflow/status"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.currentStep").value("PAYMENT"))
            .andExpect(jsonPath("$.cancelled").value(false))
            .andExpect(jsonPath("$.progress").value(33.3))
    }

    @Test
    fun `should cancel workflow`() {
        val orderId = UUID.randomUUID()
        val reason = "Customer requested cancellation"

        mockMvc
            .perform(
                put("/orders/$orderId/workflow/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(CancelWorkflowRequest(reason))),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.message").value("Cancellation signal sent"))
    }
}
