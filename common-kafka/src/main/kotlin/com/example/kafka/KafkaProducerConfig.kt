package com.example.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class KafkaProducerConfig {
    @Bean
    @ConditionalOnMissingBean(ProducerFactory::class)
    fun producerFactory(
        @Value("\${spring.kafka.producer.transaction-id-prefix:payments-tx-}") transactionIdPrefix: String,
    ): ProducerFactory<String, Any> {
        val props =
            mapOf(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE.toString(),
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5",
            )
        return DefaultKafkaProducerFactory<String, Any>(props).apply {
            setTransactionIdPrefix(transactionIdPrefix.ifBlank { "payments-tx-" })
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> =
        KafkaTemplate(producerFactory)

    @Bean
    @ConditionalOnMissingBean(KafkaTransactionManager::class)
    fun kafkaTransactionManager(producerFactory: ProducerFactory<String, Any>): KafkaTransactionManager<String, Any> =
        KafkaTransactionManager(producerFactory)

    @Bean
    fun idempotentProducerFactoryCustomizer(): DefaultKafkaProducerFactoryCustomizer =
        DefaultKafkaProducerFactoryCustomizer { factory ->
            factory.updateConfigs(
                mapOf(
                    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                    ProducerConfig.ACKS_CONFIG to "all",
                    ProducerConfig.RETRIES_CONFIG to Integer.MAX_VALUE.toString(),
                    ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5",
                ),
            )
        }
}
