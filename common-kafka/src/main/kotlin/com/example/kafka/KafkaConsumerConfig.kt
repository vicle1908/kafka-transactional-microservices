package com.example.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class KafkaConsumerConfig {
    @Bean
    @ConditionalOnMissingBean(ConsumerFactory::class)
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props =
            mapOf(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        transactionManager: KafkaTransactionManager<String, Any>?,
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory

        // Configure for transactional consumption if transaction manager is available
        if (transactionManager != null) {
            factory.containerProperties.kafkaAwareTransactionManager = transactionManager
            factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        } else {
            factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }

        return factory
    }
}
