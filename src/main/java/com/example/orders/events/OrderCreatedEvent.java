package com.example.orders.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String tenantId,
        UUID orderId,
        String customerEmail,
        BigDecimal totalAmount,
        Instant occurredAt
) {
}
