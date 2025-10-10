package com.example.inventory.config

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.transaction.KafkaAwareTransactionManager
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class InventoryKafkaConfig {
    @Bean
    fun kafkaTransactionManager(
        producerFactory: ProducerFactory<String, String>,
    ): KafkaTransactionManager<String, String> = KafkaTransactionManager(producerFactory)

    @Bean
    fun kafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        consumerFactory: ConsumerFactory<String, String>,
        transactionManager: KafkaAwareTransactionManager<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        @Suppress("UNCHECKED_CAST")
        val factory =
            ConcurrentKafkaListenerContainerFactory<String, String>().also {
                configurer.configure(
                    it as ConcurrentKafkaListenerContainerFactory<Any, Any>,
                    consumerFactory as ConsumerFactory<Any, Any>,
                )
            }
        factory.setConcurrency(1)
        factory.containerProperties.kafkaAwareTransactionManager = transactionManager
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }
}
