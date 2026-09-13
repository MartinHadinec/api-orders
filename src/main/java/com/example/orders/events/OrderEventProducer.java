package com.example.orders.events;

import java.time.Instant;
import java.util.UUID;

import com.example.orders.entity.Order;
import com.example.orders.entity.OutboxEvent;
import com.example.orders.repositories.OutboxEventsRepository;
import com.example.orders.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String TOPIC = "orders.created.v1";
    public static final String AGGREGATE_TYPE = "Order";
    public static final String EVENT_TYPE_ORDER_CREATED = "OrderCreated";

    private final OutboxEventsRepository outboxEventsRepository;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(Order order) {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                TenantContext.require(),
                order.getId(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                Instant.now());

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(eventId);
        outbox.setTenantId(event.tenantId());
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(order.getId());
        outbox.setEventType(EVENT_TYPE_ORDER_CREATED);
        outbox.setTopic(TOPIC);
        outbox.setMessageKey(event.tenantId() + ":" + order.getId());
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outboxEventsRepository.save(outbox);
    }
}
