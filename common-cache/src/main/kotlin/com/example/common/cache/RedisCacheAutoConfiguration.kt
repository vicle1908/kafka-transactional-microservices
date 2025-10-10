package com.example.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory::class)
@EnableCaching
class RedisCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory::class)
    fun redisConnectionFactory(): RedisConnectionFactory {
        val host = System.getenv("REDIS_HOST") ?: "localhost"
        val port = (System.getenv("REDIS_PORT") ?: "6379").toIntOrNull() ?: 6379
        val username = System.getenv("REDIS_USERNAME")
        val password = System.getenv("REDIS_PASSWORD")
        val useSsl = (System.getenv("REDIS_SSL") ?: "false").equals("true", ignoreCase = true)

        val standalone = RedisStandaloneConfiguration(host, port)
        if (!username.isNullOrBlank()) {
            standalone.username = username
        }
        if (!password.isNullOrBlank()) {
            standalone.setPassword(RedisPassword.of(password))
        }

        val clientConfigBuilder = LettuceClientConfiguration.builder()
        if (useSsl) clientConfigBuilder.useSsl()
        val clientConfig = clientConfigBuilder.build()
        return LettuceConnectionFactory(standalone, clientConfig)
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager::class)
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        objectMapperProvider: ObjectProvider<ObjectMapper>,
    ): CacheManager {
        val mapper =
            objectMapperProvider
                .getIfAvailable { ObjectMapper() }
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        val keyPair = SerializationPair.fromSerializer(StringRedisSerializer())
        val valuePair = SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer(mapper))

        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(keyPair)
                .serializeValuesWith(valuePair)
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build()
    }
}
