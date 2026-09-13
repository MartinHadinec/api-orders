package com.example.orders.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param publishInterval delay between publisher runs
 * @param batchSize max rows taken per run
 * @param sendTimeout how long to wait for a Kafka acknowledgement per message
 */
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        @DefaultValue("500ms") Duration publishInterval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("10s") Duration sendTimeout
) {
}
