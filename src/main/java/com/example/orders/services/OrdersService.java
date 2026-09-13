package com.example.orders.services;

import java.util.Optional;
import java.util.UUID;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderDTO;
import com.example.orders.entity.OrderSummaryDTO;
import com.example.orders.events.OrderEventProducer;
import com.example.orders.repositories.OrdersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final OrderEventProducer eventProducer;
    private final ModelMapper modelMapper;

    @Transactional
    public OrderDTO create(OrderDTO orderDTO) {
        Order order = modelMapper.map(orderDTO, Order.class);
        order.getItems().forEach(item -> item.setOrder(order));
        Order saved = ordersRepository.save(order);

        eventProducer.publishOrderCreated(saved);

        log.info("Order created - id: {}, items: {}", saved.getId(), saved.getItems().size());
        return modelMapper.map(saved, OrderDTO.class);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> retrieveAll(Pageable pageable) {
        return ordersRepository.findSummaries(pageable);
    }

    /**
     * The Detail entity requires items, hence the @EntityGraph variant. A foreign order “does not exist” due to
     * the tenant filter; the controller returns a 404 error for this, not a 403.
     */
    @Transactional(readOnly = true)
    public Optional<OrderDTO> retrieveById(UUID id) {
        return ordersRepository.findWithItemsById(id).map(order -> modelMapper.map(order, OrderDTO.class));
    }
}
