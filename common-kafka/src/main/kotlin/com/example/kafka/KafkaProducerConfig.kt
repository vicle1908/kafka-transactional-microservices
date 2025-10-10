package com.example.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class KafkaProducerConfig {

    companion object {
        private const val MAX_IN_FLIGHT_REQUESTS = 5
    }
    @Bean
    @ConditionalOnMissingBean(ProducerFactory::class)
    fun producerFactory(
        kafkaProperties: KafkaProperties,
    ): ProducerFactory<String, Any> {
        val props = kafkaProperties.buildProducerProperties()
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.RETRIES_CONFIG] = Integer.MAX_VALUE
        props[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = 5

        val transactionIdPrefix =
            kafkaProperties.producer.transactionIdPrefix?.takeIf { it.isNotBlank() } ?: "payments-tx-"

        return DefaultKafkaProducerFactory<String, Any>(props).apply {
            setTransactionIdPrefix(transactionIdPrefix)
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
