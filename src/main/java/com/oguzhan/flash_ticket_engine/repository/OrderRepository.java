package com.oguzhan.flash_ticket_engine.repository;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTicketId(Long ticketId);
    long countByTicketIdAndStatus(Long ticketId, OrderStatus status);
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM Order o WHERE o.ticket.id = :ticketId AND o.status = :status")
    int sumQuantityByTicketIdAndStatus(@Param("ticketId") Long ticketId, @Param("status") OrderStatus status);
}
