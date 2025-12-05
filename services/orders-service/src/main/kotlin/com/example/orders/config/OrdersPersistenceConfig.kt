package com.example.orders.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class OrdersPersistenceConfig {
    @Bean(name = ["transactionManager"])
    @Primary
    fun jpaTransactionManager(
        entityManagerFactory: EntityManagerFactory,
    ): JpaTransactionManager = JpaTransactionManager(entityManagerFactory)
}
