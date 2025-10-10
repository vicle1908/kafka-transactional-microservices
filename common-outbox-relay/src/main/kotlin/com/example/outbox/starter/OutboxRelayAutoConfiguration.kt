package com.example.outbox.starter

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@AutoConfiguration
@ComponentScan(basePackages = ["com.example.outbox"])
@EntityScan(basePackages = ["com.example.outbox.entity"])
@EnableJpaRepositories(basePackages = ["com.example.outbox.repository"])
class OutboxRelayAutoConfiguration
