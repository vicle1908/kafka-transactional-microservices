package com.example.notification.config

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class NotificationKafkaConfig {
    @Bean
    fun kafkaTransactionManager(producerFactory: ProducerFactory<String, Any>): KafkaTransactionManager<String, Any> =
        KafkaTransactionManager(producerFactory)

    @Bean(name = ["notificationKafkaListenerContainerFactory"])
    fun notificationKafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        consumerFactory: ConsumerFactory<*, *>,
        transactionManager: KafkaTransactionManager<String, Any>,
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
        factory.containerProperties.transactionManager = transactionManager
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }
}
