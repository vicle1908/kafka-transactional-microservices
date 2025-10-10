package com.example.inventory.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class InventoryPersistenceConfig {
    @Bean(name = ["transactionManager"])
    @Primary
    fun jpaTransactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager =
        JpaTransactionManager(entityManagerFactory)
}
