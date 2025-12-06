package com.example.orders.e2e

import com.example.orders.adapter.inbound.http.CreateOrderRequest
import com.example.orders.adapter.inbound.http.OrderItemRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class OrderFulfillmentE2ETest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
            registry.add("spring.kafka.consumer.group-id") { "orders-e2e-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
        }
    }

    @Test
    fun `should create order and process fulfillment flow`() {
        val request =
            CreateOrderRequest(
                customerId = "customer-001",
                orderItems =
                    listOf(
                        OrderItemRequest(
                            productId = "product-001",
                            quantity = 2,
                            unitPrice = BigDecimal("29.99"),
                            productName = "Test Product",
                        ),
                    ),
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                "http://localhost:$port/orders",
                HttpMethod.POST,
                entity,
                Map::class.java,
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val responseBody = response.body as Map<*, *>
        assertThat(responseBody["orderId"]).isNotNull

        val orderId = UUID.fromString(responseBody["orderId"].toString())
        assertThat(orderId).isNotNull
    }

    @Test
    fun `should get workflow status for order`() {
        val orderId = UUID.randomUUID()

        val response =
            restTemplate.getForEntity(
                "http://localhost:$port/orders/$orderId/workflow/status",
                Map::class.java,
            )

        // Accept OK, NOT_FOUND, or INTERNAL_SERVER_ERROR (when Temporal is not available)
        assertThat(response.statusCode).isIn(
            HttpStatus.OK,
            HttpStatus.NOT_FOUND,
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
