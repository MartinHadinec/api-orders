package com.example.orders.entity;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemDTO {

    @NotBlank
    private String productName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
}
