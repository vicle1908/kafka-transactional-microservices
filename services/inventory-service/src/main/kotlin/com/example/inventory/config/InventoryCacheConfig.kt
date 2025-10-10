package com.example.inventory.config

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import java.time.Duration

@Configuration
class InventoryCacheConfig {
    @Bean
    fun inventoryCacheTtlCustomizer(): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer { builder ->
            val stockBySkuConfig =
                RedisCacheConfiguration
                    .defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(3))
                    .disableCachingNullValues()
            builder.withInitialCacheConfigurations(
                mapOf(
                    "inventory:stock:by-sku" to stockBySkuConfig,
                ),
            )
        }
}
