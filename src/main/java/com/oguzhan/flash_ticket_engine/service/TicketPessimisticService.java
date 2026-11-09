package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.response.OrderResponse;
import com.oguzhan.flash_ticket_engine.exception.InsufficientStockException;
import com.oguzhan.flash_ticket_engine.exception.TicketNotFoundException;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPessimisticService {
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse buyTicket(Long ticketId, BuyTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdWithPessimisticLock(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.getAvailableCapacity() < request.quantity()) {
            throw new InsufficientStockException(ticket.getAvailableCapacity());
        }

        ticket.setAvailableCapacity(ticket.getAvailableCapacity() - request.quantity());
        ticketRepository.save(ticket);

        Order order = Order.builder()
                .ticket(ticket)
                .userId(request.userId())
                .quantity(request.quantity())
                .totalPrice(ticket.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .status(OrderStatus.CONFIRMED)
                .build();

        orderRepository.save(order);

        return new OrderResponse(
                order.getId(),
                ticketId,
                order.getUserId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
