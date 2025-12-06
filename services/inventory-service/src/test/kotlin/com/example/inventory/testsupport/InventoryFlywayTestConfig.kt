package com.example.inventory.testsupport

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class InventoryFlywayTestConfig : FlywayConfigurationCustomizer {
    override fun customize(configuration: FluentConfiguration) {
        try {
            val method = configuration.javaClass.getMethod("databaseType", String::class.java)
            method.invoke(configuration, "postgresql")
        } catch (_: NoSuchMethodException) {
            // Flyway version does not expose databaseType(String); ignore.
        }
    }
}
