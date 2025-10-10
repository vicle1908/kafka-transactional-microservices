package com.example.payments.config

import com.example.payments.adapter.outbound.gateway.HttpPaymentGateway
import com.example.payments.adapter.outbound.gateway.HttpRefundGateway
import com.example.payments.adapter.outbound.gateway.InMemoryPaymentGateway
import com.example.payments.adapter.outbound.gateway.InMemoryRefundGateway
import com.example.payments.application.port.out.PaymentGateway
import com.example.payments.application.port.out.RefundGateway
import io.netty.channel.ChannelOption
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

@Configuration
@EnableConfigurationProperties(PaymentGatewayProperties::class, RefundGatewayProperties::class)
class PaymentGatewayConfig(
    private val webClientBuilder: WebClient.Builder,
) {
    @Bean
    fun paymentGateway(properties: PaymentGatewayProperties): PaymentGateway =
        when (properties.mode) {
            PaymentGatewayProperties.Mode.IN_MEMORY -> InMemoryPaymentGateway(properties.inMemory)
            PaymentGatewayProperties.Mode.HTTP ->
                HttpPaymentGateway(
                    webClient =
                        buildWebClient(
                            baseUrl = properties.http.baseUrl,
                            connectTimeout = properties.http.connectTimeout,
                            readTimeout = properties.http.readTimeout,
                            apiKeyHeader = properties.http.apiKeyHeader,
                            apiKey = properties.http.apiKey,
                        ),
                    properties = properties.http,
                )
        }

    @Bean
    fun refundGateway(properties: RefundGatewayProperties): RefundGateway =
        when (properties.mode) {
            RefundGatewayProperties.Mode.IN_MEMORY -> InMemoryRefundGateway(properties.inMemory)
            RefundGatewayProperties.Mode.HTTP ->
                HttpRefundGateway(
                    webClient =
                        buildWebClient(
                            baseUrl = properties.http.baseUrl,
                            connectTimeout = properties.http.connectTimeout,
                            readTimeout = properties.http.readTimeout,
                            apiKeyHeader = properties.http.apiKeyHeader,
                            apiKey = properties.http.apiKey,
                        ),
                    properties = properties.http,
                )
        }

    private fun buildWebClient(
        baseUrl: String,
        connectTimeout: Duration,
        readTimeout: Duration,
        apiKeyHeader: String?,
        apiKey: String?,
    ): WebClient {
        require(baseUrl.isNotBlank()) {
            "HTTP gateway base-url must not be blank when HTTP mode is enabled"
        }
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val connectMillis =
            connectTimeout
                .toMillis()
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        val httpClient =
            HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMillis)
                .responseTimeout(readTimeout)

        val builder =
            webClientBuilder
                .clone()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .baseUrl(sanitizedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

        if (!apiKey.isNullOrBlank() && !apiKeyHeader.isNullOrBlank()) {
            builder.defaultHeader(apiKeyHeader, apiKey)
        }

        return builder.build()
    }
}

@ConfigurationProperties(prefix = "payments.gateway")
data class PaymentGatewayProperties(
    val mode: Mode = Mode.IN_MEMORY,
    val inMemory: InMemory = InMemory(),
    val http: Http = Http(),
) {
    enum class Mode {
        IN_MEMORY,
        HTTP,
    }

    data class InMemory(
        val approvalLimit: BigDecimal = BigDecimal("1000.00"),
        val declinedOrderIds: Set<UUID> = emptySet(),
    )

    data class Http(
        val baseUrl: String = "http://localhost:8085",
        val chargePath: String = "/payments/charge",
        val apiKeyHeader: String = "X-API-Key",
        val apiKey: String? = null,
        val connectTimeout: Duration = Duration.ofSeconds(2),
        val readTimeout: Duration = Duration.ofSeconds(5),
        val retries: Int = 0,
    )
}

@ConfigurationProperties(prefix = "payments.refund")
data class RefundGatewayProperties(
    val mode: Mode = Mode.IN_MEMORY,
    val inMemory: InMemory = InMemory(),
    val http: Http = Http(),
) {
    enum class Mode {
        IN_MEMORY,
        HTTP,
    }

    data class InMemory(
        val declinedPaymentIds: Set<UUID> = emptySet(),
    )

    data class Http(
        val baseUrl: String = "http://localhost:8085",
        val refundPath: String = "/payments/refund",
        val apiKeyHeader: String = "X-API-Key",
        val apiKey: String? = null,
        val connectTimeout: Duration = Duration.ofSeconds(2),
        val readTimeout: Duration = Duration.ofSeconds(5),
        val retries: Int = 0,
    )
}
