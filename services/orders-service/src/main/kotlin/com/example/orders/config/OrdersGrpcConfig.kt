@file:Suppress("MagicNumber")

package com.example.orders.config

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Configuration
@EnableConfigurationProperties(OrdersGrpcProperties::class)
class OrdersGrpcConfig(
    private val properties: OrdersGrpcProperties,
    private val grpcServices: List<BindableService>,
) {
    private val logger = LoggerFactory.getLogger(OrdersGrpcConfig::class.java)

    @Bean
    fun ordersGrpcServer(): Server {
        val builder = ServerBuilder.forPort(properties.port)
        grpcServices.forEach(builder::addService)
        return builder.build()
    }

    @Bean
    fun ordersGrpcLifecycle(server: Server): SmartLifecycle =
        object : SmartLifecycle {
            private val running = AtomicBoolean(false)

            override fun start() {
                if (running.compareAndSet(false, true)) {
                    server.start()
                    val actualPort = server.port
                    logger.info("Orders gRPC server started on port {}", actualPort)
                }
            }

            override fun stop() {
                if (running.compareAndSet(true, false)) {
                    server.shutdown()
                    if (!server.awaitTermination(properties.shutdownGracePeriod.toMillis(), TimeUnit.MILLISECONDS)) {
                        logger.warn(
                            "Orders gRPC server did not shut down within {} ms; forcing shutdown",
                            properties.shutdownGracePeriod.toMillis(),
                        )
                        server.shutdownNow()
                    }
                }
            }

            override fun stop(callback: Runnable) {
                stop()
                callback.run()
            }

            override fun isRunning(): Boolean = running.get()

            override fun isAutoStartup(): Boolean = true

            override fun getPhase(): Int = 0
        }
}

@ConfigurationProperties(prefix = "orders.grpc")
data class OrdersGrpcProperties(
    val port: Int = 9090,
    val shutdownGracePeriod: Duration = Duration.ofSeconds(5),
)
