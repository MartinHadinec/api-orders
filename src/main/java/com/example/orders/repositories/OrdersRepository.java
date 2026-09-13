package com.example.orders.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderSummaryDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrdersRepository extends JpaRepository<Order, UUID> {

    @EntityGraph("Order.withItems")
    Optional<Order> findWithItemsById(UUID id);

    @Query("""
            select new com.example.orders.entity.OrderSummaryDTO(
                o.id, o.customerEmail, o.totalAmount, count(i))
            from Order o left join o.items i
            group by o.id, o.customerEmail, o.totalAmount
            """)
    Page<OrderSummaryDTO> findSummaries(Pageable pageable);
}
