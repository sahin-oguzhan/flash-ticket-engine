package com.oguzhan.flash_ticket_engine.repository;

import com.oguzhan.flash_ticket_engine.domain.entity.OrderNaive;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderNaiveRepository extends JpaRepository<OrderNaive, Long> {
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM OrderNaive o WHERE o.ticket.id = :ticketId AND o.status = :status")
    int sumQuantityByTicketIdAndStatus(@Param("ticketId") Long ticketId, @Param("status") OrderStatus status);

    long countByTicketIdAndStatus(Long ticketId, OrderStatus status);
}
