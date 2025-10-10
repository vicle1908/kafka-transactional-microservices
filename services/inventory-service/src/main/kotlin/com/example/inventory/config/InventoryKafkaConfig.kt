package com.example.inventory.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.transaction.KafkaAwareTransactionManager
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class InventoryKafkaConfig {
    @Bean
    fun kafkaTransactionManager(
        producerFactory: ProducerFactory<String, String>,
    ): KafkaTransactionManager<String, String> = KafkaTransactionManager(producerFactory)
}
