package com.example.orders.events;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.orders.config.OutboxProperties;
import com.example.orders.entity.OutboxEvent;
import com.example.orders.repositories.OutboxEventsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    public static final String TENANT_HEADER = "x-tenant-id";
    public static final String EVENT_TYPE_HEADER = "event-type";
    public static final String EVENT_ID_HEADER = "event-id";

    private final OutboxEventsRepository outboxEventsRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${outbox.publish-interval:500ms}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventsRepository.findPendingForUpdate(properties.batchSize());
        for (OutboxEvent outbox : pending) {
            try {
                kafkaTemplate.send(toMessage(outbox)).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                outbox.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                // stop the batch to keep per-key ordering; the rest is retried on the next run
                log.warn("Publishing outbox event {} failed, will retry", outbox.getId(), ex);
                return;
            }
        }
        if (!pending.isEmpty()) {
            log.debug("Published {} outbox events", pending.size());
        }
    }

    private Message<String> toMessage(OutboxEvent outbox) {
        assert outbox.getId() != null;
        return MessageBuilder
                .withPayload(outbox.getPayload())
                .setHeader(KafkaHeaders.TOPIC, outbox.getTopic())
                .setHeader(KafkaHeaders.KEY, outbox.getMessageKey())
                .setHeader(TENANT_HEADER, outbox.getTenantId())
                .setHeader(EVENT_TYPE_HEADER, outbox.getEventType())
                .setHeader(EVENT_ID_HEADER, outbox.getId().toString())
                .build();
    }
}
