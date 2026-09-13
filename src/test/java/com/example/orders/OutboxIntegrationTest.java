package com.example.orders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.example.orders.entity.OutboxEvent;
import com.example.orders.events.OrderEventProducer;
import com.example.orders.events.OutboxPublisher;
import com.example.orders.repositories.OutboxEventsRepository;
import com.example.orders.tenant.TenantFilter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class OutboxIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    OutboxEventsRepository outboxEventsRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaConnectionDetails kafkaConnectionDetails;

    @Test
    void createdOrderIsPublishedThroughOutbox() {
        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(OrderEventProducer.TOPIC));

            HttpHeaders headers = new HttpHeaders();
            headers.set(TenantFilter.TENANT_HEADER, "acme");
            ResponseEntity<JsonNode> created = rest.postForEntity("/api/v1/orders",
                    new HttpEntity<>(Map.of("customerEmail", "outbox@acme.cz", "totalAmount", 42), headers),
                    JsonNode.class);
            assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            UUID orderId = UUID.fromString(created.getBody().get("id").asString());

            // the outbox row exists and gets marked as published
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                OutboxEvent row = outboxEventsRepository.findAll().stream()
                        .filter(e -> orderId.equals(e.getAggregateId()))
                        .findFirst().orElseThrow();
                assertThat(row.getEventType()).isEqualTo(OrderEventProducer.EVENT_TYPE_ORDER_CREATED);
                assertThat(row.getPublishedAt()).isNotNull();
            });

            // the message reached Kafka with the composite key and tenant headers
            String expectedKey = "acme:" + orderId;
            ConsumerRecord<String, String> record = await().atMost(Duration.ofSeconds(15))
                    .until(() -> findRecord(consumer, expectedKey), r -> r != null);
            // jsonb normalizes formatting and key order, so compare parsed fields, not raw text
            JsonNode payload = objectMapper.readTree(record.value());
            assertThat(payload.get("orderId").asString()).isEqualTo(orderId.toString());
            assertThat(payload.get("tenantId").asString()).isEqualTo("acme");
            assertThat(payload.get("eventId").asString()).isEqualTo(header(record, OutboxPublisher.EVENT_ID_HEADER));
            assertThat(header(record, OutboxPublisher.TENANT_HEADER)).isEqualTo("acme");
            assertThat(header(record, OutboxPublisher.EVENT_TYPE_HEADER)).isEqualTo(OrderEventProducer.EVENT_TYPE_ORDER_CREATED);
        }
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    private ConsumerRecord<String, String> findRecord(KafkaConsumer<String, String> consumer, String key) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
            if (key.equals(record.key())) {
                return record;
            }
        }
        return null;
    }

    private KafkaConsumer<String, String> consumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                String.join(",", kafkaConnectionDetails.getConsumer().getBootstrapServers()));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
