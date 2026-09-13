package com.example.orders.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryDTO(UUID id, String customerEmail, BigDecimal totalAmount, long itemCount) {
}
