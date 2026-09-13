package com.example.orders.controllers;

import static org.springframework.http.HttpHeaders.LOCATION;

import java.net.URI;
import java.util.UUID;

import com.example.orders.entity.OrderDTO;
import com.example.orders.entity.OrderSummaryDTO;
import com.example.orders.services.OrdersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Orders for the Current Tenant")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    @PostMapping
    @Operation(summary = "Creates a new order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failure or missing X-Tenant-ID",
                    content = @Content)
    })
    public ResponseEntity<OrderDTO> post(@RequestBody @Valid OrderDTO orderDTO) {
        OrderDTO createdOrder = ordersService.create(orderDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdOrder.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(LOCATION, location.toString())
                .body(createdOrder);
    }

    @GetMapping
    @Operation(summary = "Retrieves orders of the current tenant, paged")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Missing X-Tenant-ID", content = @Content)
    })
    public ResponseEntity<Page<OrderSummaryDTO>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<OrderSummaryDTO> orders = ordersService.retrieveAll(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves an order by id, including items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Missing X-Tenant-ID", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not Found - Order does not exist for this tenant",
                    content = @Content)
    })
    public ResponseEntity<OrderDTO> getById(@PathVariable UUID id) {
        return ordersService.retrieveById(id)
                .map(orderDTO -> ResponseEntity.status(HttpStatus.OK).body(orderDTO))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
