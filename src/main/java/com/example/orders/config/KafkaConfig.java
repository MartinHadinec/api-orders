package com.example.orders.config;

import com.example.orders.events.OrderEventProducer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic ordersCreatedTopic() {
        return TopicBuilder.name(OrderEventProducer.TOPIC).partitions(3).replicas(1).build();
    }
}
