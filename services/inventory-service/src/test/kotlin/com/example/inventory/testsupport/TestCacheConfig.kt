package com.example.inventory.testsupport

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class TestCacheConfig {
    @Bean
    fun cacheManager(): CacheManager = ConcurrentMapCacheManager()
}
