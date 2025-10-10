package com.example.payments.adapter.outbound.gateway

import com.example.payments.application.port.out.PaymentChargeRequest
import com.example.payments.application.port.out.PaymentChargeResult
import com.example.payments.config.PaymentGatewayConfig
import com.example.payments.config.PaymentGatewayProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

class HttpPaymentGatewayTest {
    private val server = MockWebServer()

    @BeforeEach
    fun setUp() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `charge returns approved when provider approves`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"APPROVED","confirmationCode":"CONF-123"}""")
                .setHeader("Content-Type", "application/json"),
        )

        val gateway = newGateway()
        val result =
            gateway.charge(
                PaymentChargeRequest(
                    paymentId = UUID.randomUUID(),
                    orderId = UUID.randomUUID(),
                    amount = BigDecimal("42.00"),
                ),
            )

        assertThat(result).isInstanceOf(PaymentChargeResult.Approved::class.java)
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/payments/charge")
        assertThat(recorded.getHeader("X-API-Key")).isEqualTo("test-key")
    }

    @Test
    fun `charge returns declined when provider declines`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"DECLINED","declineReason":"insufficient funds"}""")
                .setHeader("Content-Type", "application/json"),
        )

        val gateway = newGateway()
        val result =
            gateway.charge(
                PaymentChargeRequest(
                    paymentId = UUID.randomUUID(),
                    orderId = UUID.randomUUID(),
                    amount = BigDecimal("11.00"),
                ),
            )

        assertThat(result).isInstanceOf(PaymentChargeResult.Declined::class.java)
        assertThat((result as PaymentChargeResult.Declined).reason).contains("insufficient")
    }

    @Test
    fun `charge retries before succeeding`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"APPROVED","confirmationCode":"CONF-999"}""")
                .setHeader("Content-Type", "application/json"),
        )

        val gateway = newGateway(retries = 1)
        val result =
            gateway.charge(
                PaymentChargeRequest(
                    paymentId = UUID.randomUUID(),
                    orderId = UUID.randomUUID(),
                    amount = BigDecimal("5.25"),
                ),
            )

        assertThat(result).isInstanceOf(PaymentChargeResult.Approved::class.java)
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `charge returns decline when server error persists`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("internal failure"),
        )

        val gateway = newGateway()
        val result =
            gateway.charge(
                PaymentChargeRequest(
                    paymentId = UUID.randomUUID(),
                    orderId = UUID.randomUUID(),
                    amount = BigDecimal("9.99"),
                ),
            )

        assertThat(result).isInstanceOf(PaymentChargeResult.Declined::class.java)
        assertThat((result as PaymentChargeResult.Declined).reason).contains("internal failure")
    }

    private fun newGateway(retries: Int = 0): HttpPaymentGateway {
        val baseUrl = server.url("/").toString().trimEnd('/')
        val properties =
            PaymentGatewayProperties(
                mode = PaymentGatewayProperties.Mode.HTTP,
                http =
                    PaymentGatewayProperties.Http(
                        baseUrl = baseUrl,
                        chargePath = "/payments/charge",
                        apiKeyHeader = "X-API-Key",
                        apiKey = "test-key",
                        connectTimeout = Duration.ofSeconds(1),
                        readTimeout = Duration.ofSeconds(2),
                        retries = retries,
                    ),
            )
        val config = PaymentGatewayConfig(WebClient.builder())
        val gateway = config.paymentGateway(properties)
        return gateway as HttpPaymentGateway
    }
}
