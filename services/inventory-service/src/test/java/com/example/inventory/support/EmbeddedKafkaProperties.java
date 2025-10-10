package com.example.inventory.support;

import java.util.function.Supplier;
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition;

public final class EmbeddedKafkaProperties {
    private EmbeddedKafkaProperties() {}

    public static Supplier<Object> bootstrapServersSupplier() {
        return () -> EmbeddedKafkaCondition.getBroker().getBrokersAsString();
    }
}
