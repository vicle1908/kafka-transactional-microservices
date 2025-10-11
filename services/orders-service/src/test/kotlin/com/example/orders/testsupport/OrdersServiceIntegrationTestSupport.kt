package com.example.orders.testsupport

import io.temporal.client.WorkflowClient
import org.flywaydb.core.Flyway
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@Import(TestCacheConfig::class, OrdersServiceIntegrationTestSupport.WorkflowClientStubConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class OrdersServiceIntegrationTestSupport {
    companion object {
        @Container
        private val postgres =
            PostgreSQLContainer("postgres:16.3-alpine").apply {
                withDatabaseName("orders_test")
                withUsername("postgres")
                withPassword("postgres")
            }

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.flyway.clean-disabled") { false }
        }
    }

    @TestConfiguration
    class FlywayTestConfig {
        @Bean
        fun cleanMigrateStrategy(): FlywayMigrationStrategy =
            FlywayMigrationStrategy { flyway: Flyway ->
                flyway.clean()
                flyway.migrate()
            }
    }

    @TestConfiguration
    class WorkflowClientStubConfig {
        @Bean
        fun workflowClient(): WorkflowClient = mock()
    }
}
